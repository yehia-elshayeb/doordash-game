package game.GUI;

import java.io.IOException;

import game.engine.Role;
import game.engine.monsters.Monster;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class GameApp extends Application {

    public static final int W = 1280;
    public static final int H = 800;

    static final Color BG_TOP    = Color.web("#0F1B3D");
    static final Color BG_BOTTOM = Color.web("#1E2761");
    static final Color CORAL     = Color.web("#F96167");
    static final Color GOLD      = Color.web("#F9E795");
    static final Color ICE       = Color.web("#CADCFC");
    static final Color SCARER_C  = Color.web("#3D4FA8");
    static final Color LAUGHER_C = Color.web("#D4A017");

    private Stage stage;

    @Override
    public void start(Stage s) {
        this.stage = s;
        s.setTitle("DoorDasH - Scare vs Laugh Touchdown");
        s.setResizable(false);
        showStart();
        s.show();
    }

    public void showStart() { stage.setScene(startScene()); }

    public void showGame(Role role) {
        try {
            stage.setScene(new GameView(this, role).getScene());
        } catch (IOException e) {
            popup("Failed to load data",
                "Could not read game data.\n\n" + e.getMessage()
                + "\n\nEnsure cards.csv, cells.csv and monsters.csv are in the working directory.");
        }
    }

    public void showEnd(Monster winner, Monster loser) {
        stage.setScene(endScene(winner, loser));
    }

    private Scene startScene() {
        Label title = new Label("DoorDasH");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 72));
        title.setTextFill(Color.WHITE);
        DropShadow ds = new DropShadow(28, Color.web("#000000", 0.55));
        ds.setOffsetY(4);
        title.setEffect(ds);

        Label subtitle = new Label("Scare vs Laugh Touchdown");
        subtitle.setFont(Font.font("Georgia", 22));
        subtitle.setTextFill(ICE);

        Label pick = new Label("Choose your side");
        pick.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        pick.setTextFill(Color.WHITE);

        ToggleGroup g = new ToggleGroup();
        RadioButton scarer  = roleRadio("SCARER",  SCARER_C, g, true);
        RadioButton laugher = roleRadio("LAUGHER", LAUGHER_C, g, false);
        HBox roles = new HBox(40, scarer, laugher);
        roles.setAlignment(Pos.CENTER);

        Label instr = new Label(
            "Each turn: optionally activate your power-up (-500 energy), then roll the dice.\n" +
            "Same-role doors boost your team. Opposing doors drain energy.\n" +
            "Monster Cells, Card Cells, Conveyor Belts and Contamination Socks all have effects.\n" +
            "Reach cell 99 with 1000+ energy to win.");
        instr.setFont(Font.font("Verdana", 13));
        instr.setTextFill(ICE);
        instr.setTextAlignment(TextAlignment.CENTER);

        Button start = bigBtn("START GAME", CORAL);
        start.setOnAction(e -> showGame(scarer.isSelected() ? Role.SCARER : Role.LAUGHER));

        VBox col = new VBox(22, title, subtitle, new Label(),
            pick, roles, new Label(), instr, new Label(), start);
        col.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(col);
        root.setBackground(bgGradient());

        FadeTransition f = new FadeTransition(Duration.millis(420), col);
        f.setFromValue(0); f.setToValue(1); f.play();
        ScaleTransition sc = new ScaleTransition(Duration.millis(520), title);
        sc.setFromX(0.85); sc.setFromY(0.85); sc.setToX(1); sc.setToY(1); sc.play();

        return new Scene(root, W, H);
    }

    private Scene endScene(Monster winner, Monster loser) {
        Label headline = new Label("VICTORY");
        headline.setFont(Font.font("Georgia", FontWeight.BOLD, 80));
        headline.setTextFill(GOLD);
        DropShadow glow = new DropShadow(40, GOLD.deriveColor(0, 1, 1, 0.65));
        glow.setSpread(0.35);
        headline.setEffect(glow);

        Label name = new Label(winner.getName());
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 36));
        name.setTextFill(Color.WHITE);

        Label info = new Label("Original role: " + winner.getOriginalRole()
            + "    Type: " + GameView.typeOf(winner));
        info.setFont(Font.font("Verdana", 16));
        info.setTextFill(ICE);

        Label energies = new Label(
            winner.getName() + " - " + winner.getEnergy() + " energy\n"
            + loser.getName() + " - " + loser.getEnergy() + " energy");
        energies.setFont(Font.font("Verdana", 18));
        energies.setTextFill(Color.WHITE);
        energies.setTextAlignment(TextAlignment.CENTER);

        Button back = bigBtn("Return to Start", CORAL);
        back.setOnAction(e -> showStart());

        VBox col = new VBox(22, headline, name, info, new Label(), energies, new Label(), back);
        col.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(col);
        root.setBackground(bgGradient());

        ScaleTransition sc = new ScaleTransition(Duration.millis(600), headline);
        sc.setFromX(0.5); sc.setFromY(0.5); sc.setToX(1); sc.setToY(1); sc.play();
        FadeTransition f = new FadeTransition(Duration.millis(700), col);
        f.setFromValue(0); f.setToValue(1); f.play();

        return new Scene(root, W, H);
    }

    private RadioButton roleRadio(String text, Color color, ToggleGroup g, boolean selected) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(g);
        rb.setSelected(selected);
        rb.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        rb.setTextFill(Color.WHITE);
        rb.setStyle("-fx-padding: 4 12 4 4;");
        DropShadow ds = new DropShadow(8, color.deriveColor(0, 1, 1, 0.7));
        rb.setEffect(ds);
        return rb;
    }

    static Button bigBtn(String text, Color color) {
        Button b = new Button(text);
        b.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        b.setStyle(bigStyle(color, false));
        DropShadow ds = new DropShadow(14, color.deriveColor(0, 1, 1, 0.65));
        b.setEffect(ds);
        b.setOnMouseEntered(e -> b.setStyle(bigStyle(color, true)));
        b.setOnMouseExited(e ->  b.setStyle(bigStyle(color, false)));
        return b;
    }

    private static String bigStyle(Color color, boolean hover) {
        Color c = hover ? color.brighter() : color;
        return "-fx-background-color: " + toRgba(c) + ";"
            + "-fx-text-fill: white; -fx-padding: 12 30 12 30;"
            + "-fx-background-radius: 8; -fx-cursor: hand;";
    }

    public static Background bgGradient() {
        LinearGradient lg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, BG_TOP), new Stop(1, BG_BOTTOM));
        return new Background(new BackgroundFill(lg, CornerRadii.EMPTY, Insets.EMPTY));
    }

    public static void popup(String title, String message) {
        Stage p = new Stage();
        p.initModality(Modality.APPLICATION_MODAL);
        p.setTitle(title);
        p.setResizable(false);

        Label head = new Label(title);
        head.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        head.setTextFill(Color.WHITE);

        Label msg = new Label(message);
        msg.setWrapText(true);
        msg.setMaxWidth(420);
        msg.setFont(Font.font("Verdana", 13));
        msg.setTextFill(ICE);
        msg.setTextAlignment(TextAlignment.CENTER);

        Button ok = new Button("OK");
        ok.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        ok.setStyle("-fx-background-color: " + toRgba(CORAL) + ";"
            + "-fx-text-fill: white; -fx-padding: 8 28 8 28;"
            + "-fx-background-radius: 6;");
        ok.setOnAction(e -> p.close());

        VBox box = new VBox(16, head, msg, ok);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28));
        StackPane root = new StackPane(box);
        root.setBackground(bgGradient());

        ScaleTransition sc = new ScaleTransition(Duration.millis(220), box);
        sc.setFromX(0.85); sc.setFromY(0.85); sc.setToX(1); sc.setToY(1);
        sc.play();

        p.setScene(new Scene(root, 480, 240));
        p.showAndWait();
    }

    public static String toRgba(Color c) {
        return String.format("rgba(%d,%d,%d,%.2f)",
            (int)(c.getRed()*255), (int)(c.getGreen()*255),
            (int)(c.getBlue()*255), c.getOpacity());
    }

    public static void main(String[] args) { launch(args); }
}