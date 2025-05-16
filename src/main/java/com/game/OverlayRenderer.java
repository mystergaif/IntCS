package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

public class OverlayRenderer implements Disposable {

    private ShapeRenderer shapeRenderer;
    private Color overlayColor;

    private Stage stage;
    private Skin skin;
    private Label exitLabel;
    private TextButton yesButton;
    private TextButton noButton;

    private MyGame game; // Добавляем ссылку на MyGame

    public OverlayRenderer(MyGame game) { // Изменяем конструктор для приема MyGame
        this.game = game; // Сохраняем ссылку
        shapeRenderer = new ShapeRenderer();
        // Цвет полупрозрачного черного (RGBA)
        overlayColor = new Color(0f, 0f, 0f, 0.7f); // 70% непрозрачность

        stage = new Stage();
        // Создаем простой скин. В реальном приложении лучше использовать файл скина.
        skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));

        // Добавляем шрифт по умолчанию
        skin.add("default-font", new com.badlogic.gdx.graphics.g2d.BitmapFont());

        // Добавляем стиль для кнопок
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.checked = skin.newDrawable("white", Color.BLUE);
        textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);
        textButtonStyle.font = skin.getFont("default-font");
        skin.add("default", textButtonStyle);

        // Добавляем стиль для метки
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);


        exitLabel = new Label("Are you sure you want to exit?", skin);
        yesButton = new TextButton("Yes", skin);
        noButton = new TextButton("No", skin);

        // Добавляем слушателей событий для кнопок
        yesButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit(); // Выход из приложения при нажатии "Да"
            }
        });

        noButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.hideOverlay(); // Вызываем метод в MyGame для скрытия оверлея
            }
        });

        // Создаем таблицу для размещения элементов
        Table table = new Table();
        table.setFillParent(true); // Растягиваем таблицу на весь экран

        // Добавляем элементы в таблицу
        table.add(exitLabel).padBottom(20).row(); // Метка, отступ снизу, перенос строки
        table.add(yesButton).width(100).padRight(20); // Кнопка "Да", ширина, отступ справа
        table.add(noButton).width(100); // Кнопка "Нет", ширина

        // Добавляем таблицу на сцену
        stage.addActor(table);

        // Устанавливаем обработчик ввода для сцены
        Gdx.input.setInputProcessor(stage);
    }

    public void render() {
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeType.Filled);
        shapeRenderer.setColor(overlayColor);
        // Рисуем прямоугольник на весь экран
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();

        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

        // Отрисовываем сцену с UI элементами
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        shapeRenderer.dispose();
        stage.dispose(); // Освобождаем ресурсы сцены
        skin.dispose();  // Освобождаем ресурсы скина
    }

    public Stage getStage() {
        return stage;
    }
}