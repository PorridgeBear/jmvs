package com.adcworks.jmvs.render;

import com.adcworks.jmvs.core.model.BoundingSphere;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/**
 * Orbit camera around the structure: left-drag rotates, right-drag or scroll zooms, middle-drag or shift+left-drag
 * pans. A left click without dragging is reported to the {@link ClickListener}.
 */
public final class OrbitCameraController extends InputAdapter {

    @FunctionalInterface
    public interface ClickListener {
        void clicked(int screenX, int screenY);
    }

    private static final float FIELD_OF_VIEW = 40f;
    private static final float ROTATE_DEGREES_PER_PIXEL = 0.4f;
    private static final int CLICK_TOLERANCE_PIXELS = 4;

    private final PerspectiveCamera camera;
    private final ClickListener clickListener;
    private final Vector3 target = new Vector3();
    private final Vector3 tmp = new Vector3();
    private float radius = 10;
    private float distance = 30;
    private int button = -1;
    private int downX, downY, lastX, lastY;
    private boolean dragged;
    private boolean shiftPressed;

    public OrbitCameraController(ClickListener clickListener) {
        this.camera = new PerspectiveCamera(FIELD_OF_VIEW, 1, 1);
        this.clickListener = clickListener;
    }

    public PerspectiveCamera camera() {
        return camera;
    }

    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = Math.max(1, height);
        camera.update();
    }

    /**
     * Looks at the sphere's centre from +z at a distance where the whole sphere fits the view.
     */
    public void frame(BoundingSphere sphere) {
        target.set(sphere.x(), sphere.y(), sphere.z());
        radius = Math.max(sphere.radius(), 1f);
        float aspect = camera.viewportWidth / camera.viewportHeight;
        float halfAngle = Math.min(FIELD_OF_VIEW, FIELD_OF_VIEW * aspect) / 2 * MathUtils.degreesToRadians;
        distance = radius / MathUtils.sin(halfAngle) * 1.05f;
        camera.position.set(target).add(0, 0, distance);
        camera.up.set(0, 1, 0);
        camera.lookAt(target);
        updateClipping();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        this.button = button;
        downX = lastX = screenX;
        downY = lastY = screenY;
        dragged = false;
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        int dx = screenX - lastX, dy = screenY - lastY;
        lastX = screenX;
        lastY = screenY;
        if (Math.abs(screenX - downX) > CLICK_TOLERANCE_PIXELS || Math.abs(screenY - downY) > CLICK_TOLERANCE_PIXELS) {
            dragged = true;
        }
        if (button == Input.Buttons.MIDDLE || (button == Input.Buttons.LEFT && shiftPressed)) {
            pan(dx, dy);
        } else if (button == Input.Buttons.LEFT) {
            rotate(dx, dy);
        } else if (button == Input.Buttons.RIGHT) {
            zoom(1 + dy * 0.005f);
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT && !dragged && clickListener != null) {
            clickListener.clicked(screenX, screenY);
        }
        this.button = -1;
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        zoom(1 + amountY * 0.1f);
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT) {
            shiftPressed = true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT) {
            shiftPressed = false;
        }
        return false;
    }

    private void rotate(int dx, int dy) {
        rotate(-dx * ROTATE_DEGREES_PER_PIXEL, -dy * ROTATE_DEGREES_PER_PIXEL);
    }

    /**
     * Orbits around the target: {@code yawDegrees} about the camera's up axis, {@code pitchDegrees} about its
     * right axis.
     */
    public void rotate(float yawDegrees, float pitchDegrees) {
        tmp.set(camera.direction).crs(camera.up).nor();
        camera.rotateAround(target, camera.up, yawDegrees);
        camera.rotateAround(target, tmp, pitchDegrees);
        camera.update();
    }

    private void zoom(float factor) {
        distance = MathUtils.clamp(distance * factor, radius * 0.05f + 1f, radius * 20f);
        camera.position.set(target).mulAdd(camera.direction, -distance);
        updateClipping();
    }

    private void pan(int dx, int dy) {
        float unitsPerPixel = 2 * distance * MathUtils.tan(FIELD_OF_VIEW / 2 * MathUtils.degreesToRadians)
                / camera.viewportHeight;
        tmp.set(camera.direction).crs(camera.up).nor().scl(-dx * unitsPerPixel);
        tmp.mulAdd(camera.up, dy * unitsPerPixel);
        target.add(tmp);
        camera.position.add(tmp);
        camera.update();
    }

    private void updateClipping() {
        camera.near = Math.max(0.1f, distance - radius * 2.5f);
        camera.far = distance + radius * 2.5f;
        camera.update();
    }
}
