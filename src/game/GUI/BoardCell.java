package game.GUI;

import game.engine.Role;
import game.engine.cells.Cell;
import game.engine.cells.CardCell;
import game.engine.cells.ContaminationSock;
import game.engine.cells.ConveyorBelt;
import game.engine.cells.DoorCell;
import game.engine.cells.MonsterCell;
import game.engine.cells.TransportCell;
import game.engine.monsters.Monster;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class BoardCell extends StackPane {

    public static final double SIZE = 58;

    private final int index;

    public BoardCell(int index) {
        this.index = index;
        setMinSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);
        setBorder(new Border(new BorderStroke(Color.web("#1A1F40"),
            BorderStrokeStyle.SOLID, new CornerRadii(4), new BorderWidths(0.6))));
    }

    public int getIndex() { return index; }

    public void render(Cell cell, Monster player, Monster opponent) {
        getChildren().clear();
        Color base = colorFor(cell);
        boolean used = cell instanceof DoorCell && ((DoorCell) cell).isActivated();
        if (used) base = base.deriveColor(0, 0.6, 0.45, 1);

        LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, base.brighter()), new Stop(1, base.darker()));
        setBackground(new Background(new BackgroundFill(grad, new CornerRadii(4), Insets.EMPTY)));

        Label idx = small(String.valueOf(index), 9,
            luminance(base) < 0.55 ? Color.web("#E8ECF7") : Color.web("#1A1F40"));
        StackPane.setAlignment(idx, Pos.TOP_LEFT);
        StackPane.setMargin(idx, new Insets(2, 0, 0, 4));
        getChildren().add(idx);

        VBox center = new VBox(0);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(9, 2, 2, 2));

        if (cell instanceof DoorCell) {
            DoorCell d = (DoorCell) cell;
            center.getChildren().addAll(
                small(d.getRole() == Role.SCARER ? "SCARE" : "LAUGH", 8, Color.WHITE),
                small(String.valueOf(d.getEnergy()), 15, Color.WHITE));
            if (used) center.getChildren().add(small("USED", 7, Color.web("#FFCDD6")));
        } else if (cell instanceof CardCell) {
            center.getChildren().add(small("CARD", 11, Color.WHITE));
        } else if (cell instanceof ConveyorBelt) {
            int e = ((TransportCell) cell).getEffect();
            center.getChildren().addAll(
                small("CONV", 8, Color.WHITE),
                small((e > 0 ? "+" : "") + e, 13, Color.WHITE));
        } else if (cell instanceof ContaminationSock) {
            int e = ((TransportCell) cell).getEffect();
            center.getChildren().addAll(
                small("SOCK", 8, Color.WHITE),
                small(String.valueOf(e), 13, Color.WHITE));
        } else if (cell instanceof MonsterCell) {
            Monster m = ((MonsterCell) cell).getCellMonster();
            center.getChildren().addAll(
                small("MON", 8, Color.WHITE),
                small(initials(m.getName()), 11, Color.WHITE));
        }
        if (!center.getChildren().isEmpty()) getChildren().add(center);

        HBox markers = new HBox(3);
        markers.setAlignment(Pos.BOTTOM_RIGHT);
        markers.setPadding(new Insets(0, 4, 4, 0));
        if (player != null && player.getPosition() == index)
            markers.getChildren().add(marker(player, true));
        if (opponent != null && opponent.getPosition() == index)
            markers.getChildren().add(marker(opponent, false));
        if (!markers.getChildren().isEmpty()) {
            StackPane.setAlignment(markers, Pos.BOTTOM_RIGHT);
            getChildren().add(markers);
        }
    }

    private static Label small(String text, double size, Color color) {
        Label l = new Label(text);
        l.setFont(Font.font("Verdana", FontWeight.BOLD, size));
        l.setTextFill(color);
        return l;
    }

    private static StackPane marker(Monster m, boolean isPlayer) {
        Color c = isPlayer ? Color.web("#4DA3FF") : Color.web("#FF6B5B");
        Circle dot = new Circle(10, c);
        dot.setStroke(Color.WHITE);
        dot.setStrokeWidth(1.8);
        DropShadow ds = new DropShadow(6, c.deriveColor(0, 1, 1.1, 0.9));
        ds.setSpread(0.25);
        dot.setEffect(ds);
        Label t = new Label(String.valueOf(m.getName().charAt(0)));
        t.setFont(Font.font("Verdana", FontWeight.BOLD, 11));
        t.setTextFill(Color.WHITE);
        return new StackPane(dot, t);
    }

    private static Color colorFor(Cell c) {
        if (c instanceof DoorCell)
            return ((DoorCell) c).getRole() == Role.SCARER
                ? GameApp.SCARER_C : GameApp.LAUGHER_C;
        if (c instanceof CardCell)          return Color.web("#A14271");
        if (c instanceof ConveyorBelt)      return Color.web("#2DA6BD");
        if (c instanceof ContaminationSock) return Color.web("#C12B2B");
        if (c instanceof MonsterCell)       return Color.web("#2C8C3F");
        return Color.web("#3A4470");
    }

    private static double luminance(Color c) {
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }

    private static String initials(String name) {
        StringBuilder s = new StringBuilder();
        boolean next = true;
        for (int i = 0; i < name.length() && s.length() < 3; i++) {
            char ch = name.charAt(i);
            if (Character.isWhitespace(ch)) { next = true; continue; }
            if (next && Character.isLetter(ch)) {
                s.append(Character.toUpperCase(ch));
                next = false;
            }
        }
        return s.toString();
    }
}