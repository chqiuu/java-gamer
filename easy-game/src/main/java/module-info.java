module com.chqiuu.gamer.easygame {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    opens com.chqiuu.gamer.easygame to javafx.fxml;
    exports com.chqiuu.gamer.easygame;
}