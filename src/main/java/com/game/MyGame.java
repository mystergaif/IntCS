package com.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.files.FileHandle;

import java.util.Random;
import java.io.BufferedReader;
import java.io.IOException;

public class MyGame extends ApplicationAdapter implements com.badlogic.gdx.InputProcessor {
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Model model;
    private Array<ModelInstance> cubes;
    private Pixmap grassPixmap;
    private Texture grassTexture;
    private Material grassMaterial;
    private Material brickMaterial;
    private Model brickModel;
    private OverlayRenderer overlayRenderer;
    private boolean showOverlay = false;

    private com.badlogic.gdx.InputProcessor defaultInputProcessor;

    private Vector3 playerPosition;
    private Vector3 playerVelocity;
    private float moveSpeed = 3.33f;
    private float gravity = -9.8f;
    private float mouseSensitivity = 1.1f;

    // Variables for camera rotation angles
    private float yaw = 0f;   // Horizontal angle, in degrees
    private float pitch = 0f; // Vertical angle, in degrees

    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = sensitivity;
    }

    public float getMouseSensitivity() {
        return this.mouseSensitivity;
    }

    private Environment environment;
    private DirectionalLight directionalLight;

    // New directional shadow light (sun)
    private DirectionalShadowLight shadowLight;
    private ModelBatch shadowBatch;

    @Override
    public void create () {
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(16f / 2f, 1.8f, 16f / 2f);
        camera.lookAt(16f / 2f, 1.8f, 16f / 2f - 1f);
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        modelBatch = new ModelBatch();

        // Create shadow batch for rendering shadows
        shadowBatch = new ModelBatch();

        // Create shadow light representing the sun
        shadowLight = new DirectionalShadowLight(1024, 1024, 100f, 100f, 1f, 300f);
        shadowLight.setDirection(-1f, -0.8f, -0.2f);
        shadowLight.setColor(Color.WHITE);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(shadowLight); // Add shadow light to environment

        // Create grass texture
        int textureSize = 64;
        grassPixmap = new Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888);
        Random random = new Random();

        for (int y = 0; y < textureSize; y++) {
            for (int x = 0; x < textureSize; x++) {
                int green = 100 + random.nextInt(100);
                grassPixmap.setColor(new Color(0, green / 255f, 0, 1));
                grassPixmap.drawPixel(x, y);
            }
        }

        grassTexture = new Texture(grassPixmap);
        grassMaterial = new Material(TextureAttribute.createDiffuse(grassTexture));

        // Create brick texture
        int brickTextureSize = 64;
        Pixmap brickPixmap = new Pixmap(brickTextureSize, brickTextureSize, Pixmap.Format.RGBA8888);

        for (int y = 0; y < brickTextureSize; y++) {
            for (int x = 0; x < brickTextureSize; x++) {
                int r = 178;
                int g = 34;
                int b = 34;

                if (y % 16 == 0 || x % 16 == 0) {
                    r = 139;
                    g = 0;
                    b = 0;
                }

                brickPixmap.setColor(r / 255f, g / 255f, b / 255f, 1f);
                brickPixmap.drawPixel(x, y);
            }
        }

        Texture brickTexture = new Texture(brickPixmap);
        brickPixmap.dispose();

        brickMaterial = new Material(TextureAttribute.createDiffuse(brickTexture));

        ModelBuilder modelBuilder = new ModelBuilder();
        model = modelBuilder.createBox(1f, 1f, 1f,
                grassMaterial,
                Usage.Position | Usage.Normal | Usage.TextureCoordinates);

        brickModel = modelBuilder.createBox(1f, 1f, 1f,
                brickMaterial,
                Usage.Position | Usage.Normal | Usage.TextureCoordinates);

        cubes = new Array<ModelInstance>();

        playerPosition = new Vector3(camera.position);
        playerVelocity = new Vector3();

        Gdx.input.setCursorCatched(true);

        overlayRenderer = new OverlayRenderer(this);

        defaultInputProcessor = Gdx.input.getInputProcessor();

        // Clear cubes before loading map
        cubes.clear();

        loadMap("floor.txt", -1f);
        loadWalls("walls.txt", 0f);
        loadWalls("walls.txt", 1f);
        loadWalls("walls.txt", 2f);
    }

    private void loadMap(String filePath, float yOffset) {
        FileHandle file = Gdx.files.internal(filePath);
        if (!file.exists()) {
            Gdx.app.error("MyGame", "Map file not found: " + filePath);
            return;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(file.reader());
            String line;
            int z = 0;
            while ((line = reader.readLine()) != null) {
                for (int x = 0; x < line.length(); x++) {
                    char character = line.charAt(x);
                    if (character == '#') {
                        ModelInstance cube = new ModelInstance(model);
                        cube.transform.setToTranslation(x, yOffset, z);
                        cubes.add(cube);
                    } else if (character == '&') {
                        playerPosition.set(x, yOffset + 1.0f, z);
                        ModelInstance cube = new ModelInstance(brickModel);
                        cube.transform.setToTranslation(x, yOffset, z);
                        cubes.add(cube);
                    }
                }
                z++;
            }
        } catch (IOException e) {
            Gdx.app.error("MyGame", "Error loading map file: " + filePath, e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                Gdx.app.error("MyGame", "Error closing map file: " + filePath, e);
            }
        }
    }

    private void loadWalls(String filePath, float yOffset) {
        FileHandle file = Gdx.files.internal(filePath);
        if (!file.exists()) {
            Gdx.app.error("MyGame", "Walls file not found: " + filePath);
            return;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(file.reader());
            String line;
            int z = 0;
            while ((line = reader.readLine()) != null) {
                for (int x = 0; x < line.length(); x++) {
                    char character = line.charAt(x);
                    if (character == '#') {
                        ModelInstance cube = new ModelInstance(brickModel);
                        cube.transform.setToTranslation(x, yOffset, z);
                        cubes.add(cube);
                    } else if (character == '&' && yOffset == 0f) {
                        ModelInstance cube = new ModelInstance(brickModel);
                        cube.transform.setToTranslation(x, yOffset, z);
                        cubes.add(cube);
                    }
                }
                z++;
            }
        } catch (IOException e) {
            Gdx.app.error("MyGame", "Error loading walls file: " + filePath, e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                Gdx.app.error("MyGame", "Error closing walls file: " + filePath, e);
            }
        }
    }

    @Override
    public void render () {
        float deltaTime = Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            showOverlay = !showOverlay;
            if (showOverlay) {
                Gdx.input.setCursorCatched(false);
                Gdx.input.setInputProcessor(overlayRenderer.getStage());
            } else {
                Gdx.input.setCursorCatched(true);
                Gdx.input.setInputProcessor(this);
            }
        }

        if (!showOverlay) {
            // Get raw mouse delta
            float rawDeltaX = Gdx.input.getDeltaX();
            float rawDeltaY = Gdx.input.getDeltaY();

            // Update yaw and pitch based on raw mouse movement and sensitivity
            yaw += rawDeltaX * mouseSensitivity;
            pitch -= rawDeltaY * mouseSensitivity;

            // Clamp pitch to avoid flipping
            pitch = Math.max(-90f, Math.min(90f, pitch));

            // Calculate new direction vector from yaw and pitch (convert degrees to radians)
            float yawRad = (float) Math.toRadians(yaw);
            float pitchRad = (float) Math.toRadians(pitch);

            float x = (float) (Math.cos(pitchRad) * Math.cos(yawRad));
            float y = (float) Math.sin(pitchRad);
            float z = (float) (Math.cos(pitchRad) * Math.sin(yawRad));

            camera.direction.set(x, y, z).nor();
            camera.update();

            Vector3 direction = new Vector3();
            if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                direction.add(camera.direction.cpy().nor());
            }
            if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                direction.sub(camera.direction.cpy().nor());
            }
            if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                direction.sub(camera.direction.cpy().crs(camera.up).nor());
            }
            if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                direction.add(camera.direction.cpy().crs(camera.up).nor());
            }
            direction.y = 0;
            direction.nor().scl(moveSpeed * deltaTime);

            Vector3 nextPositionX = new Vector3(playerPosition.x + direction.x, playerPosition.y, playerPosition.z);
            Vector3 nextPositionZ = new Vector3(playerPosition.x, playerPosition.y, playerPosition.z + direction.z);

            // Remove collision checks so camera/player can move freely without blocking
            playerPosition.x += direction.x;
            playerPosition.z += direction.z;

            playerVelocity.y += gravity * deltaTime;
            playerPosition.add(playerVelocity.cpy().scl(deltaTime));

            int blockX = (int) Math.floor(playerPosition.x);
            int blockZ = (int) Math.floor(playerPosition.z);

            if (blockX >= 0 && blockX < 48 && blockZ >= 0 && blockZ < 48) {
                if (playerVelocity.y < 0 && playerPosition.y <= 1.0f) {
                    playerPosition.y = 1.0f;
                    playerVelocity.y = 0;
                }
            }

            // Update camera position independently of rotation
            camera.position.set(playerPosition);
            camera.update();
        }

        // Render shadow map first
        shadowLight.begin(Vector3.Zero, camera.direction);
        shadowBatch.begin(shadowLight.getCamera());
        for (ModelInstance cube : cubes) {
            shadowBatch.render(cube);
        }
        shadowBatch.end();
        shadowLight.end();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.5f, 0.7f, 1.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        for (ModelInstance cube : cubes) {
            modelBatch.render(cube, environment);
        }
        modelBatch.end();

        if (showOverlay) {
            overlayRenderer.render();
        }
    }

    @Override
    public void resize(int width, int height) {
        shadowLight.getCamera().viewportWidth = width;
        shadowLight.getCamera().viewportHeight = height;
        shadowLight.getCamera().update();
        overlayRenderer.resize(width, height);
    }

    @Override
    public void dispose () {
        modelBatch.dispose();
        shadowBatch.dispose();
        model.dispose();
        grassTexture.dispose();
        grassPixmap.dispose();
        overlayRenderer.dispose();
        shadowLight.dispose();
    }

    public void hideOverlay() {
        showOverlay = false;
        Gdx.input.setCursorCatched(true);
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public boolean keyDown(int keycode) { return false; }
    @Override
    public boolean keyUp(int keycode) { return false; }
    @Override
    public boolean keyTyped(char character) { return false; }
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override
    public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override
    public boolean scrolled(float amountX, float amountY) { return false; }
}
