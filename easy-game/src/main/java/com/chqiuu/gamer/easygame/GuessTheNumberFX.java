package com.chqiuu.gamer.easygame;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Random;

public class GuessTheNumberFX extends Application {

    private enum Difficulty {
        EASY("简单 (1-50)", 1, 50, 8),
        MEDIUM("中等 (1-100)", 1, 100, 7),
        HARD("困难 (1-500)", 1, 500, 9); // 范围更大，次数相对也多点

        final String label;
        final int min;
        final int max;
        final int maxAttempts;

        Difficulty(String label, int min, int max, int maxAttempts) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.maxAttempts = maxAttempts;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private Difficulty currentDifficulty = Difficulty.MEDIUM; // 默认难度
    private int secretNumber;
    private int attemptsLeft;
    private boolean gameOver = false;

    // --- JavaFX UI Elements ---
    private ComboBox<Difficulty> difficultyComboBox;
    private Label instructionLabel;
    private TextField guessInput;
    private Button guessButton;
    private Label feedbackLabel;
    private Label attemptsLabel;
    private TextArea historyTextArea;
    private Button newGameButton;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("猜数字游戏 - JavaFX版");

        // --- Layout Panes ---
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15)); // 窗口内边距

        VBox topPane = createTopPane();
        VBox centerPane = createCenterPane();
        VBox bottomPane = createBottomPane();

        root.setTop(topPane);
        BorderPane.setAlignment(topPane, Pos.CENTER);
        BorderPane.setMargin(topPane, new Insets(0, 0, 15, 0)); // 顶部面板下边距

        root.setCenter(centerPane);
        BorderPane.setAlignment(centerPane, Pos.CENTER);

        root.setBottom(bottomPane);
        BorderPane.setAlignment(bottomPane, Pos.CENTER);
        BorderPane.setMargin(bottomPane, new Insets(15, 0, 0, 0)); // 底部面板上边距

        // --- Event Handlers ---
        difficultyComboBox.setOnAction(e -> {
            currentDifficulty = difficultyComboBox.getValue();
            startNewGame(); // 改变难度立即开始新游戏
        });

        guessButton.setOnAction(e -> checkGuess());
        guessInput.setOnAction(e -> checkGuess()); // 允许回车提交
        newGameButton.setOnAction(e -> startNewGame());

        // --- Initialize and Show ---
        startNewGame(); // 开始第一局游戏

        Scene scene = new Scene(root, 450, 500); // 调整窗口大小以容纳历史记录
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // 可以禁止调整大小
        primaryStage.show();
    }

    // --- Helper methods to create UI sections ---

    private VBox createTopPane() {
        VBox vbox = new VBox(10); // 垂直间距 10
        vbox.setAlignment(Pos.CENTER);

        Label difficultyLabel = new Label("选择难度:");
        difficultyComboBox = new ComboBox<>();
        difficultyComboBox.getItems().addAll(Difficulty.values());
        difficultyComboBox.setValue(currentDifficulty); // 设置默认选中

        HBox difficultyBox = new HBox(10, difficultyLabel, difficultyComboBox);
        difficultyBox.setAlignment(Pos.CENTER);

        instructionLabel = new Label(); // 内容在 startNewGame 中设置
        instructionLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        instructionLabel.setWrapText(true); // 允许换行

        vbox.getChildren().addAll(difficultyBox, instructionLabel);
        return vbox;
    }

    private VBox createCenterPane() {
        VBox vbox = new VBox(15); // 垂直间距 15
        vbox.setAlignment(Pos.CENTER);

        HBox inputBox = new HBox(10); // 水平间距 10
        inputBox.setAlignment(Pos.CENTER);
        Label inputPrompt = new Label("输入你的猜测:");
        guessInput = new TextField();
        guessInput.setPromptText("数字"); // 提示文字
        guessInput.setPrefWidth(80);
        guessButton = new Button("猜！");
        guessButton.setDefaultButton(true); // 设为默认按钮（回车触发）
        inputBox.getChildren().addAll(inputPrompt, guessInput, guessButton);

        feedbackLabel = new Label("请开始游戏或输入猜测。");
        feedbackLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        feedbackLabel.setTextFill(Color.DARKSLATEGRAY);

        attemptsLabel = new Label(); // 内容在 startNewGame/checkGuess 中设置

        vbox.getChildren().addAll(inputBox, feedbackLabel, attemptsLabel);
        return vbox;
    }

    private VBox createBottomPane() {
        VBox vbox = new VBox(10); // 垂直间距 10
        vbox.setAlignment(Pos.CENTER);

        Label historyLabel = new Label("猜测历史:");
        historyTextArea = new TextArea();
        historyTextArea.setEditable(false);
        historyTextArea.setPrefHeight(150); // 设定历史区域高度
        historyTextArea.setWrapText(true);

        newGameButton = new Button("开始新游戏");

        vbox.getChildren().addAll(historyLabel, historyTextArea, newGameButton);
        return vbox;
    }


    // --- Game Logic Methods ---

    private void startNewGame() {
        gameOver = false;
        attemptsLeft = currentDifficulty.maxAttempts;
        Random random = new Random();
        secretNumber = random.nextInt(currentDifficulty.max - currentDifficulty.min + 1) + currentDifficulty.min;

        // --- Reset UI ---
        instructionLabel.setText("我已经想好了一个 " + currentDifficulty.min + " 到 " + currentDifficulty.max + " 之间的数字！");
        feedbackLabel.setText("游戏开始！你有 " + attemptsLeft + " 次机会。");
        feedbackLabel.setTextFill(Color.DARKBLUE);
        attemptsLabel.setText("剩余尝试次数: " + attemptsLeft);
        guessInput.clear();
        guessInput.setDisable(false); // 启用输入
        guessButton.setDisable(false); // 启用按钮
        historyTextArea.clear();
        difficultyComboBox.setDisable(false); // 允许在游戏开始前更改难度

        guessInput.requestFocus(); // 让输入框获得焦点

        // System.out.println("新游戏 (" + currentDifficulty.label + ") 开始，秘密数字: " + secretNumber); // Debug
    }

    private void checkGuess() {
        if (gameOver) return; // 如果游戏已结束，不处理猜测

        String guessText = guessInput.getText();
        int guess;

        try {
            guess = Integer.parseInt(guessText);

            if (guess < currentDifficulty.min || guess > currentDifficulty.max) {
                setFeedback("请输入 " + currentDifficulty.min + " 到 " + currentDifficulty.max + " 之间的有效数字！", Color.ORANGERED);
                guessInput.selectAll();
                guessInput.requestFocus();
                return; // 不计入尝试次数
            }

            // 猜测有效，处理逻辑
            attemptsLeft--;
            String feedback;
            Color feedbackColor;

            if (guess < secretNumber) {
                feedback = "太低了！";
                feedbackColor = Color.BLUE;
            } else if (guess > secretNumber) {
                feedback = "太高了！";
                feedbackColor = Color.ORANGE;
            } else {
                feedback = "恭喜你！猜对了！答案就是 " + secretNumber + "！";
                feedbackColor = Color.GREEN;
                gameOver = true;
            }

            // 更新历史记录和界面
            updateHistory(guess, feedback);
            setFeedback(feedback, feedbackColor);
            attemptsLabel.setText("剩余尝试次数: " + attemptsLeft);

            // 检查游戏是否结束
            if (gameOver) {
                handleGameOver(true); // 玩家获胜
            } else if (attemptsLeft <= 0) {
                setFeedback("很遗憾，你没有猜对。答案是 " + secretNumber + "。", Color.RED);
                gameOver = true;
                handleGameOver(false); // 玩家失败
            }

        } catch (NumberFormatException ex) {
            setFeedback("请输入有效的数字！", Color.RED);
        } finally {
            guessInput.selectAll(); // 选中内容方便下次输入
            guessInput.requestFocus();
        }
    }

    private void updateHistory(int guess, String feedback) {
        historyTextArea.appendText("猜测: " + guess + " -> " + feedback + "\n");
    }

    private void setFeedback(String message, Color color) {
        feedbackLabel.setText(message);
        feedbackLabel.setTextFill(color);
    }

    private void handleGameOver(boolean playerWon) {
        guessInput.setDisable(true);
        guessButton.setDisable(true);
        difficultyComboBox.setDisable(false); // 游戏结束后允许更改难度
        // 可以添加一些额外的视觉效果，比如播放声音等
    }

    // --- Main Method ---
    public static void main(String[] args) {
        launch(args);
    }
}