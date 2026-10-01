package com.bluelotuscoding.eidolonunchained.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * A buffer source that hides the parts of a model on the far side of one or more planes, the way terrain or a gate's
 * back hides them in the world: a codex page has neither, so a deity rising out of a rift or stepping through a gate
 * would show before it arrives. Each quad is cut exactly at the planes (all vertex attributes interpolated), so parts
 * appear smoothly as they cross. Planes are in the entity's own space (blocks, origin at its feet, +z the way it
 * faces), the space the page set up before handing the entity to its renderer.
 */
final class GroundClipBuffers implements MultiBufferSource {
    /** Keeps decals lying on a plane (a rift's plates) from flickering at it. */
    private static final float EPSILON = 0.01f;

    /** Keep the side where {@code normal · p >= offset}. */
    record Plane(Vector3f normal, float offset) {
        static Plane ground(float y) { return new Plane(new Vector3f(0, 1, 0), y); }
        static Plane behind(float blocks) { return new Plane(new Vector3f(0, 0, 1), -blocks); }
    }

    private final MultiBufferSource delegate;
    private final Matrix4f toLocal;
    private final List<Plane> planes;

    GroundClipBuffers(MultiBufferSource delegate, Matrix4f entitySpace, List<Plane> planes) {
        this.delegate = delegate;
        this.toLocal = new Matrix4f(entitySpace).invert();
        this.planes = planes;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        var buffer = delegate.getBuffer(type);
        return type.mode() == VertexFormat.Mode.QUADS ? new Clipper(buffer) : buffer;
    }

    private record Vert(float x, float y, float z, float r, float g, float b, float a, float u, float v,
                        int overlay, int light, float nx, float ny, float nz, Vector3f local) {
        Vert lerp(Vert o, float t) {
            return new Vert(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t,
                    r + (o.r - r) * t, g + (o.g - g) * t, b + (o.b - b) * t, a + (o.a - a) * t,
                    u + (o.u - u) * t, v + (o.v - v) * t, overlay, light, nx, ny, nz, new Vector3f(local).lerp(o.local, t));
        }
    }

    private final class Clipper implements VertexConsumer {
        private final VertexConsumer out;
        private final List<Vert> quad = new ArrayList<>(4);
        // the chained form: vertex(...).color(...).uv(...)...endVertex()
        private float x, y, z, r = 1, g = 1, b = 1, a = 1, u, v, nx, ny = 1, nz;
        private int overlay, light;

        Clipper(VertexConsumer out) {
            this.out = out;
        }

        @Override
        public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v,
                           int overlay, int light, float nx, float ny, float nz) {
            add(x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz);
        }

        @Override public VertexConsumer vertex(double x, double y, double z) { this.x = (float) x; this.y = (float) y; this.z = (float) z; return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { this.r = r / 255f; this.g = g / 255f; this.b = b / 255f; this.a = a / 255f; return this; }
        @Override public VertexConsumer uv(float u, float v) { this.u = u; this.v = v; return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { this.overlay = u | (v << 16); return this; }
        @Override public VertexConsumer uv2(int u, int v) { this.light = u | (v << 16); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { this.nx = x; this.ny = y; this.nz = z; return this; }
        @Override public void endVertex() { add(x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz); }
        @Override public void defaultColor(int r, int g, int b, int a) { out.defaultColor(r, g, b, a); }
        @Override public void unsetDefaultColor() { out.unsetDefaultColor(); }

        private void add(float x, float y, float z, float r, float g, float b, float a, float u, float v,
                         int overlay, int light, float nx, float ny, float nz) {
            var p = toLocal.transform(new Vector4f(x, y, z, 1));
            quad.add(new Vert(x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz, new Vector3f(p.x, p.y, p.z)));
            if (quad.size() == 4) {
                clip();
                quad.clear();
            }
        }

        /** Sutherland–Hodgman against each plane, then out as quads (a fan of quads, the last one doubled up if odd). */
        private void clip() {
            List<Vert> poly = quad;
            for (var plane : planes) {
                poly = clip(poly, plane);
                if (poly.size() < 3) return;
            }
            if (poly == quad) { for (var p : quad) emit(p); return; }
            for (int i = 1; i < poly.size() - 1; i += 2) {
                emit(poly.get(0)); emit(poly.get(i)); emit(poly.get(i + 1));
                emit(poly.get(i + 2 < poly.size() ? i + 2 : i + 1));
            }
        }

        private List<Vert> clip(List<Vert> in, Plane plane) {
            boolean anyOut = false;
            for (var p : in) if (distance(p, plane) < 0) { anyOut = true; break; }
            if (!anyOut) return in;
            var result = new ArrayList<Vert>(in.size() + 1);
            for (int i = 0; i < in.size(); i++) {
                Vert cur = in.get(i), next = in.get((i + 1) % in.size());
                float dc = distance(cur, plane), dn = distance(next, plane);
                if (dc >= 0) result.add(cur);
                if ((dc >= 0) != (dn >= 0)) result.add(cur.lerp(next, dc / (dc - dn)));
            }
            return result;
        }

        private float distance(Vert p, Plane plane) {
            return plane.normal().dot(p.local) - plane.offset() + EPSILON;
        }

        private void emit(Vert p) {
            out.vertex(p.x, p.y, p.z, p.r, p.g, p.b, p.a, p.u, p.v, p.overlay, p.light, p.nx, p.ny, p.nz);
        }
    }
}
