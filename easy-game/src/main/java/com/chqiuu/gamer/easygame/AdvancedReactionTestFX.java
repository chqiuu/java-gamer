package com.chqiuu.gamer.easygame;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AdvancedReactionTestFX extends Application {

    // --- 常量定义 ---
    private static final int NUM_TRIALS = 10; // 每轮测试的总试次数量
    private static final int NUM_TARGETS_TOTAL = 5; // 每次屏幕上出现的图形总数
    private static final double TARGET_SIZE = 50; // 图形的大小（像素）
    private static final double MIN_WAIT_SECONDS = 1.0; // “准备”阶段的最短等待时间（秒）
    private static final double MAX_WAIT_SECONDS = 2.5; // “准备”阶段的最长等待时间（秒）
    private static final double STIMULUS_DURATION_SECONDS = 1.5; // 图形在屏幕上显示的最长时间（秒）

    // --- 颜色定义 ---
    private static final Color TARGET_COLOR = Color.LIMEGREEN; // 正确目标的颜色
    private static final Color DISTRACTOR_COLOR_1 = Color.ROYALBLUE; // 干扰项颜色1
    private static final Color DISTRACTOR_COLOR_2 = Color.ORANGE; // 干扰项颜色2
    private static final Color BACKGROUND_COLOR = Color.DARKGRAY; // 游戏区域背景色

    // --- 游戏状态枚举 ---
    private enum GameState {
        INITIAL,        // 初始状态，等待开始
        GET_READY,      // 准备阶段，等待随机延时
        SHOWING_STIMULUS,// 显示刺激物（图形）阶段，等待点击
        TRIAL_OVER,     // 单次试次结束，短暂显示结果
        ROUND_OVER      // 整轮测试结束，显示最终结果
    }
    private GameState currentState = GameState.INITIAL; // 当前游戏状态，默认为初始状态
    private int currentTrial = 0; // 当前进行的试次数
    private int score = 0; // 当前得分
    private long totalReactionTimeMillis = 0; // 正确反应的总毫秒数，用于计算平均值
    private int correctHits = 0; // 正确点击次数
    private int incorrectHits = 0; // 错误点击次数（点中干扰项）
    private int misses = 0; // 错过次数（超时或点背景）

    private Shape targetShapeDefinition; // 本轮需要点击的目标形状的定义 (圆形或方形)
    private Shape correctTargetNode = null; // 当前屏幕上代表正确目标的那个 JavaFX 节点
    private long stimulusAppearTimeNanos = 0; // 刺激物（图形）出现的纳秒级时间戳

    private Random random = new Random(); // 用于生成随机数
    private PauseTransition waitTimer; // “准备”阶段的延迟计时器
    private Timeline stimulusTimer; // 限制刺激物显示时间的计时器

    // --- UI 元素 ---
    private BorderPane root; // 根布局面板
    private Pane gamePane; // 游戏区域，用于放置图形
    private Label instructionLabel; // 显示指示信息（如“准备”、“点击圆形”）的标签
    private Label scoreLabel; // 显示当前得分的标签
    private Label trialLabel; // 显示当前试次进度的标签
    private Label avgTimeLabel; // 显示平均反应时间的标签
    private Button startButton; // 开始/重新开始游戏的按钮

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("高级反应速度测试 (中文版)"); // 设置窗口标题

        root = new BorderPane(); // 使用 BorderPane 布局
        root.setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY))); // 设置背景色

        // --- 创建并放置顶部信息面板 ---
        VBox infoPane = createInfoPane();
        root.setTop(infoPane);
        BorderPane.setAlignment(infoPane, Pos.CENTER);
        BorderPane.setMargin(infoPane, new Insets(10)); // 设置外边距

        // --- 创建并放置中央游戏区域 ---
        gamePane = new Pane(); // 使用 Pane 布局，允许绝对定位
        gamePane.setPrefSize(600, 400); // 定义游戏区域的首选大小
        // 为游戏区域添加背景点击事件处理器，用于处理“错过”的情况
        gamePane.setOnMouseClicked(event -> {
            if (currentState == GameState.SHOWING_STIMULUS) {
                handleMiss(); // 如果在显示刺激物时点击了背景，算作错过
            }
        });
        root.setCenter(gamePane);
        BorderPane.setAlignment(gamePane, Pos.CENTER);

        // --- 创建并放置底部控制面板 ---
        HBox controlPane = createControlPane();
        root.setBottom(controlPane);
        BorderPane.setAlignment(controlPane, Pos.CENTER);
        BorderPane.setMargin(controlPane, new Insets(10)); // 设置外边距

        Scene scene = new Scene(root, 700, 600); // 创建场景，调整窗口大小以适应内容
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // 禁止调整窗口大小
        primaryStage.show(); // 显示窗口

        updateUI(); // 更新UI到初始状态
    }

    // --- UI 创建辅助方法 ---

    /**
     * 创建顶部的信息面板 (VBox)
     * @return 包含指示、试次、得分、平均时间标签的 VBox
     */
    private VBox createInfoPane() {
        VBox vbox = new VBox(10); // 垂直间距为 10
        vbox.setAlignment(Pos.CENTER); // 居中对齐
        instructionLabel = createStyledLabel("", 18, Color.WHITE); // 初始化为空，稍后设置
        trialLabel = createStyledLabel("试次: 0 / " + NUM_TRIALS, 14, Color.LIGHTGRAY);
        scoreLabel = createStyledLabel("得分: 0", 14, Color.LIGHTYELLOW);
        avgTimeLabel = createStyledLabel("平均反应时间: N/A", 14, Color.LIGHTCYAN); // N/A 表示尚无数据
        vbox.getChildren().addAll(instructionLabel, trialLabel, scoreLabel, avgTimeLabel); // 添加所有标签
        return vbox;
    }

    /**
     * 创建底部的控制面板 (HBox)
     * @return 包含开始按钮的 HBox
     */
    private HBox createControlPane() {
        HBox hbox = new HBox(); // 水平布局
        hbox.setAlignment(Pos.CENTER); // 居中对齐
        startButton = new Button("开始游戏"); // 设置按钮文本
        startButton.setFont(Font.font(16)); // 设置字体大小
        startButton.setOnAction(e -> startGame()); // 设置按钮点击事件处理器
        hbox.getChildren().add(startButton); // 添加按钮
        return hbox;
    }

    /**
     * 创建带有特定样式的标签
     * @param text 标签文本
     * @param fontSize 字体大小
     * @param color 字体颜色
     * @return 配置好样式的 Label 对象
     */
    private Label createStyledLabel(String text, int fontSize, Color color) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, fontSize)); // 设置字体、粗细、大小
        label.setTextFill(color); // 设置字体颜色
        return label;
    }


    // --- 游戏逻辑方法 ---

    /**
     * 开始一轮新游戏
     */
    private void startGame() {
        currentState = GameState.GET_READY; // 设置状态为准备
        // 重置所有统计数据
        currentTrial = 0;
        score = 0;
        correctHits = 0;
        incorrectHits = 0;
        misses = 0;
        totalReactionTimeMillis = 0;
        startButton.setDisable(true); // 禁用开始按钮，防止重复点击
        startNextTrial(); // 开始第一次试次
    }

    /**
     * 开始下一次试次（或结束游戏如果试次已满）
     */
    private void startNextTrial() {
        // 检查是否所有试次都已完成
        if (currentTrial >= NUM_TRIALS) {
            endRound(); // 结束本轮游戏
            return;
        }
        currentState = GameState.GET_READY; // 设置状态为准备
        currentTrial++; // 增加试次数
        correctTargetNode = null; // 重置上一轮的目标节点引用
        gamePane.getChildren().clear(); // 清空游戏区域中的所有图形

        // 随机决定本轮的目标形状是圆形还是方形
        targetShapeDefinition = random.nextBoolean() ? new Circle(TARGET_SIZE / 2) : new Rectangle(TARGET_SIZE, TARGET_SIZE);
        String targetName = (targetShapeDefinition instanceof Circle) ? "圆形" : "方形"; // 获取目标形状的中文名
        instructionLabel.setText("准备... 点击 " + targetName + "!"); // 更新指示信息

        updateUI(); // 更新界面上的试次计数器

        // 计算并设置随机的等待时间
        double waitSeconds = MIN_WAIT_SECONDS + random.nextDouble() * (MAX_WAIT_SECONDS - MIN_WAIT_SECONDS);
        waitTimer = new PauseTransition(Duration.seconds(waitSeconds));
        waitTimer.setOnFinished(event -> showStimulus()); // 设定延迟结束后调用 showStimulus 方法
        waitTimer.play(); // 启动延迟计时器
    }

    /**
     * 显示刺激物（图形）到游戏区域
     */
    private void showStimulus() {
        currentState = GameState.SHOWING_STIMULUS; // 设置状态为显示刺激物
        instructionLabel.setText("点击!"); // 提示用户点击

        List<Node> shapes = generateShapes(); // 生成包含目标和干扰项的图形列表
        placeShapesRandomly(shapes); // 将图形随机放置在游戏区域
        gamePane.getChildren().addAll(shapes); // 将图形添加到游戏区域面板上

        stimulusAppearTimeNanos = System.nanoTime(); // 记录图形出现的精确纳秒时间

        // 设置一个计时器，用于限制图形显示的时间
        stimulusTimer = new Timeline(new KeyFrame(Duration.seconds(STIMULUS_DURATION_SECONDS), event -> {
            // 检查计时器结束时，状态是否仍然是 SHOWING_STIMULUS (即用户尚未点击)
            if (currentState == GameState.SHOWING_STIMULUS) {
                handleMiss(); // 如果超时未点击，则算作错过
            }
        }));
        stimulusTimer.play(); // 启动显示时间限制计时器
    }

    /**
     * 生成包含一个正确目标和若干干扰项的图形列表
     * @return 包含 JavaFX Shape 节点的列表
     */
    private List<Node> generateShapes() {
        List<Node> shapes = new ArrayList<>();

        // 首先创建正确的那个目标图形
        if (targetShapeDefinition instanceof Circle) {
            correctTargetNode = new Circle(TARGET_SIZE / 2, TARGET_COLOR);
        } else {
            correctTargetNode = new Rectangle(TARGET_SIZE, TARGET_SIZE, TARGET_COLOR);
        }
        addClickHandler(correctTargetNode, true); // 为其添加点击处理器，并标记为正确目标
        shapes.add(correctTargetNode); // 添加到列表中

        // 添加干扰项图形
        for (int i = 1; i < NUM_TARGETS_TOTAL; i++) { // 从 1 开始，因为已经添加了一个目标
            Shape distractor; // 干扰项形状
            // 随机决定干扰项与目标的区别：是形状不同还是颜色不同
            boolean useDifferentShape = random.nextBoolean();
            Color distractorColor = random.nextBoolean() ? DISTRACTOR_COLOR_1 : DISTRACTOR_COLOR_2; // 随机选择一个干扰色

            if (useDifferentShape) {
                // 使用与目标形状相反的形状
                if (targetShapeDefinition instanceof Circle) { // 如果目标是圆，干扰项是方
                    distractor = new Rectangle(TARGET_SIZE, TARGET_SIZE, distractorColor);
                } else { // 如果目标是方，干扰项是圆
                    distractor = new Circle(TARGET_SIZE / 2, distractorColor);
                }
            } else {
                // 使用与目标形状相同的形状，但颜色不同
                if (targetShapeDefinition instanceof Circle) {
                    distractor = new Circle(TARGET_SIZE / 2, distractorColor);
                } else {
                    distractor = new Rectangle(TARGET_SIZE, TARGET_SIZE, distractorColor);
                }
            }

            addClickHandler(distractor, false); // 为其添加点击处理器，并标记为干扰项（非正确目标）
            shapes.add(distractor); // 添加到列表中
        }

        return shapes; // 返回包含所有图形的列表
    }

    /**
     * 将图形列表中的图形随机放置在游戏区域 (gamePane) 中，尝试避免重叠
     * @param shapes 需要放置的图形节点列表
     */
    private void placeShapesRandomly(List<Node> shapes) {
        double paneWidth = gamePane.getWidth(); // 获取游戏区域宽度
        double paneHeight = gamePane.getHeight(); // 获取游戏区域高度
        // 如果面板尺寸尚未计算出来（例如布局还未完成），使用预设值
        if (paneWidth <= 0 || paneHeight <= 0) {
            paneWidth = 600;
            paneHeight = 400;
        }

        for (Node shape : shapes) {
            double x, y;
            boolean tooClose; // 标记是否与其他图形过于接近
            int attempts = 0; // 尝试次数，防止无限循环

            // 尝试为当前图形找到一个不与其他图形重叠的位置
            do {
                tooClose = false;
                // 在有效范围内随机生成坐标 (稍微离开边缘)
                x = TARGET_SIZE + random.nextDouble() * (paneWidth - 2 * TARGET_SIZE);
                y = TARGET_SIZE + random.nextDouble() * (paneHeight - 2 * TARGET_SIZE);

                // 检查与已放置图形的距离 (简化的重叠检测)
                for (Node other : gamePane.getChildren()) { // 直接检查已在面板上的图形
                    if (shape != other) { // 避免与自身比较
                        double dx = x - (other.getLayoutX() + TARGET_SIZE / 2); // 计算中心点距离 X
                        double dy = y - (other.getLayoutY() + TARGET_SIZE / 2); // 计算中心点距离 Y
                        // 如果中心点距离小于图形尺寸的 1.5 倍，则认为太近
                        if (Math.sqrt(dx * dx + dy * dy) < TARGET_SIZE * 1.5) {
                            tooClose = true;
                            break; // 一旦发现太近，无需再与其他图形比较，重新生成坐标
                        }
                    }
                }
                attempts++;
            } while (tooClose && attempts < 20); // 限制尝试次数

            // 设置图形的布局位置 (shape 的坐标是左上角，需要调整使其中心在 x, y)
            shape.setLayoutX(x - TARGET_SIZE / 2);
            shape.setLayoutY(y - TARGET_SIZE / 2);
        }
    }


    /**
     * 为指定的图形节点添加鼠标点击事件处理器
     * @param shape 要添加处理器的图形节点 (Node)
     * @param isCorrectTarget 布尔值，指示这个图形是否是本轮的正确目标
     */
    private void addClickHandler(Node shape, boolean isCorrectTarget) {
        shape.setOnMouseClicked(event -> {
            // 确保只在刺激物显示阶段处理点击事件
            if (currentState == GameState.SHOWING_STIMULUS) {
                stopTimers(); // 停止所有正在运行的计时器 (等待计时器和刺激显示计时器)
                long reactionTimeMillis = (System.nanoTime() - stimulusAppearTimeNanos) / 1_000_000; // 计算反应时间（毫秒）

                if (isCorrectTarget) {
                    handleCorrectHit(reactionTimeMillis); // 处理正确点击
                } else {
                    handleIncorrectHit(); // 处理错误点击（点中干扰项）
                }
                event.consume(); // 阻止事件继续传播（例如传播到父容器 gamePane 的点击事件）
            }
        });
    }

    /**
     * 处理正确点击目标的情况
     * @param reactionTimeMillis 本次点击的反应时间（毫秒）
     */
    private void handleCorrectHit(long reactionTimeMillis) {
        currentState = GameState.TRIAL_OVER; // 设置状态为单次试次结束
        correctHits++; // 增加正确点击计数
        totalReactionTimeMillis += reactionTimeMillis; // 累加反应时间，用于计算平均值

        // 简单的计分逻辑：基础分100，反应越慢扣分越多，最低10分
        int trialScore = Math.max(10, 100 - (int)(reactionTimeMillis / 10));
        score += trialScore; // 增加总得分
        instructionLabel.setText("命中! +" + trialScore + "分 (" + reactionTimeMillis + "ms)"); // 显示反馈信息
        scheduleNextTrial(); // 安排下一次试次
    }

    /**
     * 处理点击了干扰项（错误目标）的情况
     */
    private void handleIncorrectHit() {
        currentState = GameState.TRIAL_OVER; // 设置状态为单次试次结束
        incorrectHits++; // 增加错误点击计数
        score -= 50; // 扣除较多分数作为惩罚
        instructionLabel.setText("点错了! -50分"); // 显示反馈信息
        scheduleNextTrial(); // 安排下一次试次
    }

    /**
     * 处理错过目标的情况（超时或点击了背景）
     */
    private void handleMiss() {
        // 防止因超时和背景点击同时触发导致重复处理
        if (currentState != GameState.SHOWING_STIMULUS) return;

        stopTimers(); // 停止所有计时器
        currentState = GameState.TRIAL_OVER; // 设置状态为单次试次结束
        misses++; // 增加错过计数
        score -= 10; // 扣除少量分数
        instructionLabel.setText("超时或错过! -10分"); // 显示反馈信息
        scheduleNextTrial(); // 安排下一次试次
    }

    /**
     * 停止所有可能正在运行的计时器
     */
    private void stopTimers() {
        if (waitTimer != null) waitTimer.stop(); // 停止“准备”阶段的计时器
        if (stimulusTimer != null) stimulusTimer.stop(); // 停止限制刺激显示时间的计时器
    }


    /**
     * 安排（短暂延迟后）开始下一次试次
     */
    private void scheduleNextTrial() {
        updateUI(); // 立刻更新界面上的分数等信息
        // 设置一个短暂的停顿（例如1秒），让玩家看到本次试次的结果，然后再开始下一次
        PauseTransition briefPause = new PauseTransition(Duration.seconds(1.0));
        briefPause.setOnFinished(event -> startNextTrial()); // 停顿结束后调用 startNextTrial
        briefPause.play(); // 启动停顿计时器
    }


    /**
     * 结束整轮游戏，显示最终结果
     */
    private void endRound() {
        currentState = GameState.ROUND_OVER; // 设置状态为整轮结束
        gamePane.getChildren().clear(); // 清空游戏区域

        // 计算平均反应时间（仅基于正确点击）
        double avgTime = (correctHits > 0) ? (double)totalReactionTimeMillis / correctHits : 0;
        // 显示最终的统计信息
        instructionLabel.setText(String.format("测试结束! 总分: %d", score));
        avgTimeLabel.setText(String.format("平均反应时间: %.0f ms", avgTime)); // %.0f 表示不带小数的浮点数
        trialLabel.setText(String.format("正确: %d, 错误: %d, 错过: %d", correctHits, incorrectHits, misses));

        startButton.setDisable(false); // 重新启用开始按钮
        startButton.setText("再玩一轮"); // 修改按钮文本，提示可以重新开始
        updateUI(); // 更新最终的得分显示
    }

    /**
     * 更新界面上动态变化的标签（得分、试次、平均时间）
     */
    private void updateUI() {
        scoreLabel.setText("得分: " + score); // 更新得分
        // 只有在游戏进行中才更新试次和平均时间，避免覆盖最终结果显示
        if (currentState != GameState.ROUND_OVER) {
            trialLabel.setText("试次: " + currentTrial + " / " + NUM_TRIALS); // 更新试次进度
            double avgTime = (correctHits > 0) ? (double)totalReactionTimeMillis / correctHits : 0; // 计算当前平均时间
            avgTimeLabel.setText(String.format("平均反应时间: %.0f ms", avgTime)); // 更新平均时间显示
        }
    }


    // --- 主方法 ---
    public static void main(String[] args) {
        launch(args); // 启动 JavaFX 应用
    }
}