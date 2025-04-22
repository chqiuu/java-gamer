package com.chqiuu.gamer.easygame;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WhackAMoleFX extends Application {

    // --- 常量定义 ---
    private static final int GRID_SIZE = 3; // 网格大小 (3x3)
    private static final double HOLE_SIZE = 110; // 洞的大小
    private static final double ITEM_SIZE_RATIO = 0.65; // 物品相对于洞的大小比例

    // 颜色定义
    private static final Color HOLE_COLOR = Color.rgb(139, 69, 19); // 洞 (棕色)
    private static final Color MOLE_COLOR = Color.rgb(238, 82, 105); // 普通地鼠 (赭色)
    private static final Color BOMB_COLOR = Color.BLACK;          // 炸弹 (黑色)
    private static final Color BONUS_COLOR = Color.GOLD;          // 奖励 (金色)
    private static final Color BACKGROUND_COLOR = Color.LIGHTGREEN; // 背景 (浅绿)
    private static final Color HIT_FLASH_COLOR = Color.LIGHTYELLOW; // 击中地鼠闪烁颜色
    private static final Color BOMB_FLASH_COLOR = Color.RED;       // 击中炸弹闪烁颜色

    // 时间和概率定义
    private static final int GAME_DURATION_SECONDS = 45; // 游戏总时长（秒）
    private static final double MIN_ITEM_UP_TIME_SECONDS = 0.45; // 物品出现最短时间
    private static final double MAX_ITEM_UP_TIME_SECONDS = 1.1; // 物品出现最长时间
    private static final double BASE_APPEAR_INTERVAL_SECONDS = 0.5; // 基础出现间隔
    private static final double BOMB_PROBABILITY = 0.15; // 出现炸弹的概率 (15%)
    private static final double BONUS_PROBABILITY = 0.05; // 出现奖励的概率 (5%) (地鼠概率 = 1 - BOMB - BONUS = 80%)

    // 得分定义
    private static final int MOLE_SCORE = 10; // 打中地鼠得分
    private static final int BOMB_PENALTY = -25; // 打中炸弹扣分
    private static final int BONUS_SCORE = 50; // 打中奖励得分

    // --- 游戏状态变量 ---
    private int score = 0;
    private int timeLeft = GAME_DURATION_SECONDS;
    private boolean gameActive = false;
    private boolean acceptingInput = true; // 是否接受玩家点击（用于炸弹惩罚）

    private Random random = new Random();
    private List<ItemHole> itemHoles = new ArrayList<>(); // 存储所有洞对象
    private Timeline gameTimer; // 游戏主计时器
    private Timeline itemSpawner; // 物品生成计时器

    // --- UI 元素 ---
    private Label scoreLabel;
    private Label timeLabel;
    private Label feedbackLabel; // 用于显示额外反馈信息（如“炸弹！”）
    private Button startButton;
    private GridPane gameGrid;

    // --- 物品类型枚举 ---
    private enum ItemType {
        MOLE, BOMB, BONUS
    }

    // --- 内部类: 代表一个洞及其中的物品 ---
    private class ItemHole {
        StackPane pane; // 包含洞和物品的面板
        Shape itemShape; // 代表物品的图形 (用 Shape 更通用)
        ItemType currentItemType = null; // 当前洞中物品的类型
        boolean itemVisible = false;
        PauseTransition hideTimer; // 控制自动隐藏
        PauseTransition flashTimer; // 控制背景闪烁恢复

        ItemHole() {
            pane = new StackPane();
            pane.setPrefSize(HOLE_SIZE, HOLE_SIZE);
            pane.setStyle("-fx-background-color: #" + HOLE_COLOR.toString().substring(2) + "; " +
                    "-fx-background-radius: " + (HOLE_SIZE / 2) + ";");

            // 创建一个通用的圆形作为物品占位符，颜色和可见性后面设置
            itemShape = new Circle(HOLE_SIZE * ITEM_SIZE_RATIO / 2);
            itemShape.setVisible(false);
            itemShape.setCursor(Cursor.HAND);
            itemShape.setMouseTransparent(true); // 让点击事件穿透到 pane，方便统一处理

            pane.getChildren().add(itemShape);

            // 在 Pane 上处理点击，这样即使物品图形没完全覆盖也能点到
            pane.setOnMouseClicked(event -> {
                if (gameActive && acceptingInput && itemVisible) {
                    handleWhack(); // 处理敲击事件
                }
            });
        }

        // 显示指定类型的物品
        void showItem(ItemType type) {
            if (!itemVisible && gameActive) {
                itemVisible = true;
                currentItemType = type;

                // 根据类型设置外观
                switch (type) {
                    case MOLE:
                        ((Circle) itemShape).setFill(MOLE_COLOR);
                        // 可以给地鼠加点细节，比如眼睛，但需要更复杂的绘图
                        break;
                    case BOMB:
                        ((Circle) itemShape).setFill(BOMB_COLOR);
                        // 可以把形状改成方的或者加引线效果
                        // itemShape = new Rectangle(HOLE_SIZE * ITEM_SIZE_RATIO * 0.8, HOLE_SIZE * ITEM_SIZE_RATIO * 0.8, BOMB_COLOR); // 示例：方形炸弹
                        break;
                    case BONUS:
                        ((Circle) itemShape).setFill(BONUS_COLOR);
                        // 可以加闪烁效果或用特殊形状
                        break;
                }

                itemShape.setVisible(true);

                // 设置随机时间后自动隐藏
                double upTime = MIN_ITEM_UP_TIME_SECONDS + random.nextDouble() * (MAX_ITEM_UP_TIME_SECONDS - MIN_ITEM_UP_TIME_SECONDS);
                hideTimer = new PauseTransition(Duration.seconds(upTime));
                hideTimer.setOnFinished(e -> hideItem(false)); // false 表示非主动敲击隐藏
                hideTimer.play();
            }
        }

        // 隐藏物品
        void hideItem(boolean whackOccurred) {
            if (itemVisible) {
                if (hideTimer != null) {
                    hideTimer.stop();
                    hideTimer = null;
                }
                itemVisible = false;
                itemShape.setVisible(false);
                currentItemType = null; // 清除当前物品类型
                // 如果是因为超时自动隐藏，而不是被敲击，可以考虑是否要扣分或有其他逻辑
                // if (!whackOccurred) { /* 处理错过逻辑 */ }
            }
        }

        // 处理敲击事件
        void handleWhack() {
            if (currentItemType == null) return; // 如果没有物品则不处理

            switch (currentItemType) {
                case MOLE:
                    score += MOLE_SCORE;
                    setFeedback("打中地鼠! +" + MOLE_SCORE, Color.GREEN);
                    flashBackground(HIT_FLASH_COLOR);
                    // (可选) 播放打中音效
                    break;
                case BOMB:
                    score += BOMB_PENALTY;
                    setFeedback("炸弹! " + BOMB_PENALTY, Color.RED);
                    flashBackground(BOMB_FLASH_COLOR);
                    triggerBombPenalty(); // 触发炸弹惩罚效果
                    // (可选) 播放爆炸音效
                    break;
                case BONUS:
                    score += BONUS_SCORE;
                    setFeedback("奖励! +" + BONUS_SCORE, Color.GOLD);
                    flashBackground(BONUS_COLOR);
                    // (可选) 播放奖励音效
                    break;
            }
            updateScoreLabel();
            hideItem(true); // true 表示是主动敲击导致隐藏
        }

        // 短暂改变背景颜色以示反馈
        void flashBackground(Color flashColor) {
            if (flashTimer != null) flashTimer.stop(); // 停止上一个闪烁

            String originalStyle = pane.getStyle();
            pane.setStyle(originalStyle.replaceFirst("-fx-background-color: #[a-fA-F0-9]+;", "-fx-background-color: #" + flashColor.toString().substring(2) + ";"));

            flashTimer = new PauseTransition(Duration.millis(150)); // 闪烁持续时间
            flashTimer.setOnFinished(e -> pane.setStyle(originalStyle)); // 恢复原背景
            flashTimer.play();
        }
    }


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("高级打地鼠游戏 (中文版)");

        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        root.setPadding(new Insets(15));

        // --- 顶部信息面板 ---
        VBox infoPane = createInfoPane();
        root.setTop(infoPane);
        BorderPane.setAlignment(infoPane, Pos.CENTER);
        BorderPane.setMargin(infoPane, new Insets(0, 0, 15, 0));

        // --- 中间游戏网格 ---
        gameGrid = createGameGrid();
        root.setCenter(gameGrid);
        BorderPane.setAlignment(gameGrid, Pos.CENTER);

        // --- 底部控制面板 ---
        HBox controlPane = createControlPane();
        root.setBottom(controlPane);
        BorderPane.setAlignment(controlPane, Pos.CENTER);
        BorderPane.setMargin(controlPane, new Insets(15, 0, 0, 0));


        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // --- UI 创建辅助方法 ---

    private VBox createInfoPane() {
        VBox vbox = new VBox(5); // 减小间距
        vbox.setAlignment(Pos.CENTER);
        scoreLabel = new Label("得分: 0");
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        timeLabel = new Label("时间: " + GAME_DURATION_SECONDS + " 秒");
        timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        feedbackLabel = new Label(" "); // 初始为空白，用于显示临时反馈
        feedbackLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        feedbackLabel.setMinHeight(20); // 给反馈标签留出空间
        vbox.getChildren().addAll(scoreLabel, timeLabel, feedbackLabel);
        return vbox;
    }

    private GridPane createGameGrid() {
        GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(20); // 增加洞间距
        gridPane.setVgap(20);

        itemHoles.clear(); // 清空旧的洞（如果重玩）
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                ItemHole itemHole = new ItemHole();
                itemHoles.add(itemHole);
                gridPane.add(itemHole.pane, col, row);
            }
        }
        return gridPane;
    }

    private HBox createControlPane() {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER);
        startButton = new Button("开始游戏");
        startButton.setFont(Font.font(18)); // 增大按钮字体
        startButton.setOnAction(e -> toggleGame());
        hbox.getChildren().add(startButton);
        return hbox;
    }

    // --- 游戏逻辑方法 ---

    private void toggleGame() {
        if (gameActive) {
            stopGame();
        } else {
            startGame();
        }
    }

    private void startGame() {
        gameActive = true;
        acceptingInput = true; // 确保开始时能接受输入
        score = 0;
        timeLeft = GAME_DURATION_SECONDS;
        startButton.setText("停止游戏");
        updateScoreLabel();
        updateTimeLabel();
        setFeedback("游戏开始!", Color.BLUE); // 清除旧反馈

        for (ItemHole ih : itemHoles) {
            ih.hideItem(false); // 隐藏所有物品
        }

        // 启动游戏倒计时器
        gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            timeLeft--;
            updateTimeLabel();
            if (timeLeft <= 0) {
                stopGame();
            }
        }));
        gameTimer.setCycleCount(Timeline.INDEFINITE);
        gameTimer.play();

        // 启动物品生成计时器
        itemSpawner = new Timeline(new KeyFrame(Duration.seconds(BASE_APPEAR_INTERVAL_SECONDS * (0.7 + random.nextDouble() * 0.6)), // 加入随机间隔
                event -> {
                    if (gameActive) {
                        popRandomItem();
                    }
                }));
        itemSpawner.setCycleCount(Timeline.INDEFINITE);
        itemSpawner.play();
    }

    private void stopGame() {
        gameActive = false;
        startButton.setText("开始游戏");

        if (gameTimer != null) gameTimer.stop();
        if (itemSpawner != null) itemSpawner.stop();

        for (ItemHole ih : itemHoles) {
            ih.hideItem(false);
        }

        setFeedback("游戏结束! 最终得分: " + score, Color.DARKMAGENTA);
        // 确保输入是可接受的，为下一轮做准备
        acceptingInput = true;
        gameGrid.setEffect(null); // 移除可能的模糊效果
    }

    /**
     * 根据概率随机选择一个物品类型
     * @return 随机选中的 ItemType
     */
    private ItemType chooseRandomItemType() {
        double chance = random.nextDouble(); // 生成 0.0 到 1.0 之间的随机数
        if (chance < BOMB_PROBABILITY) {
            return ItemType.BOMB;
        } else if (chance < BOMB_PROBABILITY + BONUS_PROBABILITY) {
            return ItemType.BONUS;
        } else {
            return ItemType.MOLE;
        }
    }

    /**
     * 从一个随机的、当前没有物品的洞中弹出一个随机类型的物品
     */
    private void popRandomItem() {
        List<ItemHole> availableHoles = new ArrayList<>();
        for (ItemHole ih : itemHoles) {
            if (!ih.itemVisible) {
                availableHoles.add(ih);
            }
        }

        if (!availableHoles.isEmpty()) {
            int randomIndex = random.nextInt(availableHoles.size());
            ItemType typeToSpawn = chooseRandomItemType(); // 决定本次生成的物品类型
            availableHoles.get(randomIndex).showItem(typeToSpawn); // 显示该类型的物品
        }
    }

    /** 更新得分标签 */
    private void updateScoreLabel() {
        scoreLabel.setText("得分: " + score);
    }

    /** 更新时间标签 */
    private void updateTimeLabel() {
        timeLabel.setText("时间: " + timeLeft + " 秒");
    }

    /**
     * 设置短暂的反馈信息和颜色
     * @param message 消息文本
     * @param color 文本颜色
     */
    private void setFeedback(String message, Color color) {
        feedbackLabel.setText(message);
        feedbackLabel.setTextFill(color);
        // 可以加一个定时器，让反馈信息在几秒后自动消失
        PauseTransition clearFeedbackTimer = new PauseTransition(Duration.seconds(1.5));
        clearFeedbackTimer.setOnFinished(e -> {
            // 检查当前反馈是否还是我们设置的这个，避免清除后续的新反馈
            if(feedbackLabel.getText().equals(message)){
                feedbackLabel.setText(" ");
            }
        });
        clearFeedbackTimer.play();
    }

    /**
     * 触发炸弹惩罚效果（例如，暂时禁止点击）
     */
    private void triggerBombPenalty() {
        acceptingInput = false; // 禁用输入
        // 添加视觉效果，比如整个游戏区域模糊或变暗
        ColorAdjust darken = new ColorAdjust();
        darken.setBrightness(-0.5); // 降低亮度
        gameGrid.setEffect(darken);

        // 设置一个短暂的暂停计时器，之后恢复输入和视觉效果
        PauseTransition penaltyTimer = new PauseTransition(Duration.seconds(0.7)); // 惩罚持续时间
        penaltyTimer.setOnFinished(e -> {
            acceptingInput = true; // 恢复输入
            gameGrid.setEffect(null); // 移除效果
        });
        penaltyTimer.play();
    }


    // --- 主方法 ---
    public static void main(String[] args) {
        launch(args);
    }
}