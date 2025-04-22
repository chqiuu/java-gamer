package com.chqiuu.gamer.easygame;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.text.NumberFormat;

public class SimpleClickerIdleFX extends Application {

    // --- 游戏核心状态变量 ---
    private long points = 0; // 当前点数 (使用 long 以支持更大数值)
    private long pointsPerClick = 1; // 每次点击增加的点数
    private long pointsPerSecond = 0; // 每秒自动增加的点数

    // --- 升级相关的状态变量 ---
    private long ppcUpgradeCost = 10; // 升级“点数/点击”的当前成本
    private long ppsUpgradeCost = 25; // 升级“点数/秒”的当前成本
    private int ppcLevel = 1; // “点数/点击”的等级
    private int ppsLevel = 0; // “点数/秒”的等级

    // --- 常量定义 ---
    private static final double UPGRADE_COST_MULTIPLIER = 1.15; // 每次升级后成本增加的倍数
    private static final long PPC_INCREASE_BASE = 1;      // 每次升级PPC增加的基础值
    private static final long PPS_INCREASE_BASE = 1;      // 每次升级PPS增加的基础值

    // --- UI 元素 ---
    private Label pointsLabel;      // 显示总点数
    private Label ppcLabel;         // 显示“点数/点击”
    private Label ppsLabel;         // 显示“点数/秒”
    private Button clickButton;      // 主要的点击按钮
    private Button ppcUpgradeButton; // 升级 PPC 的按钮
    private Button ppsUpgradeButton; // 升级 PPS 的按钮
    private Label ppcUpgradeLabel;  // 显示 PPC 升级信息
    private Label ppsUpgradeLabel;  // 显示 PPS 升级信息

    private Timeline idleTimer;     // 自动产生点数的计时器
    private NumberFormat numberFormatter; // 用于格式化数字显示

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("简单点击放置游戏 (中文版)");

        numberFormatter = NumberFormat.getNumberInstance(); // 获取默认的数字格式化器

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // --- 顶部：显示点数 ---
        VBox topPane = createTopPane();
        root.setTop(topPane);
        BorderPane.setAlignment(topPane, Pos.CENTER);

        // --- 中间：点击按钮和状态 ---
        VBox centerPane = createCenterPane();
        root.setCenter(centerPane);
        BorderPane.setAlignment(centerPane, Pos.CENTER);
        BorderPane.setMargin(centerPane, new Insets(20, 0, 20, 0));

        // --- 底部：升级区域 ---
        VBox bottomPane = createUpgradesPane();
        root.setBottom(bottomPane);
        BorderPane.setAlignment(bottomPane, Pos.CENTER);

        // --- 初始化并启动游戏循环 ---
        setupIdleTimer();
        updateUI(); // 初始化UI显示

        Scene scene = new Scene(root, 450, 500); // 设置场景大小
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // --- UI 创建辅助方法 ---

    private VBox createTopPane() {
        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        pointsLabel = new Label("点数: 0");
        pointsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        vbox.getChildren().add(pointsLabel);
        return vbox;
    }

    private VBox createCenterPane() {
        VBox vbox = new VBox(15);
        vbox.setAlignment(Pos.CENTER);

        clickButton = new Button("点我加分!");
        clickButton.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        clickButton.setPrefSize(200, 80); // 设置按钮大小
        clickButton.setOnAction(e -> handleClick()); // 设置点击事件

        HBox statsBox = new HBox(20); // 水平排列 PPC 和 PPS
        statsBox.setAlignment(Pos.CENTER);
        ppcLabel = new Label("点数/点击: 1");
        ppcLabel.setFont(Font.font(14));
        ppsLabel = new Label("点数/秒: 0");
        ppsLabel.setFont(Font.font(14));
        statsBox.getChildren().addAll(ppcLabel, ppsLabel);

        vbox.getChildren().addAll(clickButton, statsBox);
        return vbox;
    }

    private VBox createUpgradesPane() {
        VBox vbox = new VBox(15); // 垂直排列两个升级项
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(10));
        vbox.setStyle("-fx-border-color: lightgray; -fx-border-width: 1; -fx-border-radius: 5;"); // 加个边框

        // --- PPC 升级项 ---
        HBox ppcBox = new HBox(10);
        ppcBox.setAlignment(Pos.CENTER_LEFT);
        ppcUpgradeLabel = new Label(); // 信息在 updateUI 中设置
        ppcUpgradeLabel.setMinWidth(250); // 固定宽度防止按钮跳动
        ppcUpgradeButton = new Button("购买");
        ppcUpgradeButton.setOnAction(e -> buyPpcUpgrade());
        ppcBox.getChildren().addAll(ppcUpgradeLabel, ppcUpgradeButton);

        // --- PPS 升级项 ---
        HBox ppsBox = new HBox(10);
        ppsBox.setAlignment(Pos.CENTER_LEFT);
        ppsUpgradeLabel = new Label(); // 信息在 updateUI 中设置
        ppsUpgradeLabel.setMinWidth(250);
        ppsUpgradeButton = new Button("购买");
        ppsUpgradeButton.setOnAction(e -> buyPpsUpgrade());
        ppsBox.getChildren().addAll(ppsUpgradeLabel, ppsUpgradeButton);

        vbox.getChildren().addAll(ppcBox, ppsBox);
        return vbox;
    }

    // --- 游戏逻辑方法 ---

    /**
     * 处理主按钮的点击事件
     */
    private void handleClick() {
        points += pointsPerClick; // 增加点数
        updateUI(); // 更新界面显示
    }

    /**
     * 购买“点数/点击”升级
     */
    private void buyPpcUpgrade() {
        if (points >= ppcUpgradeCost) { // 检查点数是否足够
            points -= ppcUpgradeCost; // 扣除成本
            ppcLevel++; // 增加等级
            // 简单地增加 PPC，可以调整这个逻辑
            pointsPerClick = 1 + (long)Math.pow(PPC_INCREASE_BASE * 1.2, ppcLevel-1) ; // 每次点击点数稍微加速增长
            // 更新下一次升级的成本（指数增长）
            ppcUpgradeCost = (long) (10 * Math.pow(UPGRADE_COST_MULTIPLIER, ppcLevel -1));
            updateUI(); // 更新界面
        } else {
            // (可选) 可以在 statusLabel 显示“点数不足”的提示
            System.out.println("点数不足，无法购买PPC升级！");
        }
    }

    /**
     * 购买“点数/秒”升级
     */
    private void buyPpsUpgrade() {
        if (points >= ppsUpgradeCost) { // 检查点数是否足够
            points -= ppsUpgradeCost; // 扣除成本
            ppsLevel++; // 增加等级
            // 简单地增加 PPS
            pointsPerSecond += PPS_INCREASE_BASE + (ppsLevel / 2) ; // 每秒点数稳定增长，等级高了略快
            // 更新下一次升级的成本
            ppsUpgradeCost = (long) (25 * Math.pow(UPGRADE_COST_MULTIPLIER, ppsLevel));
            updateUI(); // 更新界面
        } else {
            // (可选) 提示点数不足
            System.out.println("点数不足，无法购买PPS升级！");
        }
    }

    /**
     * 设置并启动自动产生点数的计时器 (Idle Timer)
     */
    private void setupIdleTimer() {
        idleTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            points += pointsPerSecond; // 每秒增加自动产生的点数
            updateUI(); // 更新界面
        }));
        idleTimer.setCycleCount(Timeline.INDEFINITE); // 无限循环
        idleTimer.play(); // 启动计时器
    }

    /**
     * 更新所有需要动态显示的 UI 元素
     */
    private void updateUI() {
        // 使用 NumberFormat 格式化大数字，增加逗号分隔符
        pointsLabel.setText("点数: " + numberFormatter.format(points));
        ppcLabel.setText("点数/点击: " + numberFormatter.format(pointsPerClick));
        ppsLabel.setText("点数/秒: " + numberFormatter.format(pointsPerSecond));

        // 更新升级按钮的文本和状态
        ppcUpgradeLabel.setText(String.format("提升点击 Lv.%d (+%s PPC)\n成本: %s 点",
                ppcLevel,
                numberFormatter.format((long)Math.pow(PPC_INCREASE_BASE * 1.2, ppcLevel) - (long)Math.pow(PPC_INCREASE_BASE*1.2, ppcLevel -1)), // 显示下一次增加多少
                numberFormatter.format(ppcUpgradeCost)));
        ppcUpgradeButton.setDisable(points < ppcUpgradeCost); // 如果点数不足则禁用按钮

        ppsUpgradeLabel.setText(String.format("提升效率 Lv.%d (+%s PPS)\n成本: %s 点",
                ppsLevel + 1, // 显示将要达到的等级
                numberFormatter.format(PPS_INCREASE_BASE + ((ppsLevel+1) / 2) - (ppsLevel/2) ), // 显示下一次增加多少
                numberFormatter.format(ppsUpgradeCost)));
        ppsUpgradeButton.setDisable(points < ppsUpgradeCost); // 如果点数不足则禁用按钮
    }


    // --- 主方法 ---
    public static void main(String[] args) {
        launch(args); // 启动 JavaFX 应用
    }
}