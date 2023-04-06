package com.tuman;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class coctailsWindow {

    @FXML
    private Button TequilaSunrise;

    @FXML
    private Button aperol;

    @FXML
    private Button b52;

    @FXML
    private Button beetle;

    @FXML
    private Button blackRussian;

    @FXML
    private Button blueLagoon;

    @FXML
    private Button boyarsky;

    @FXML
    private Button cherryLove;

    @FXML
    private Button closeMap;

    @FXML
    private Button cubaLibra;

    @FXML
    private Button daiquiri;

    @FXML
    private Button deadMan;

    @FXML
    private Button flyToMoon;

    @FXML
    private Button ginTonic;

    @FXML
    private Button greenSpirit;

    @FXML
    private Button headShot;

    @FXML
    private Button hiroshima;

    @FXML
    private Button homeless;

    @FXML
    private Button husband;

    @FXML
    private Button longIsland;

    @FXML
    private Button margarita;

    @FXML
    private Button medusa;

    @FXML
    private Button meeting;

    @FXML
    private Button memory;

    @FXML
    private Button mohito;

    @FXML
    private Button negroni;

    @FXML
    private Button orgasm;

    @FXML
    private Button otvertka;

    @FXML
    private Button pinaColada;

    @FXML
    private Button rafaello;

    @FXML
    private Button shurup;

    @FXML
    private Button strawberryMargarita;

    @FXML
    private Button tequilaBoom;

    @FXML
    private Button whiskeyCola;

    private void openNewScene(String window, String Title) throws IOException {
    Parent root = FXMLLoader.load(getClass().getResource(window));
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setTitle(Title);
        stage.initModality(Modality.NONE);
        stage.showAndWait();
        stage.setResizable(false);

    }



    public void Close(ActionEvent actionEvent) {
        Stage stage = (Stage) closeMap.getScene().getWindow();
        stage.close();
    }

    public void whiskeyWindow(ActionEvent actionEvent) {
        whiskeyCola.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/whiskeyCola.fxml","Виски Кола");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void orgasmWindow(ActionEvent actionEvent) {
        orgasm.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/orgasm.fxml","Кричащий оргазм");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void memoryWindow(ActionEvent actionEvent) {
        memory.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/memory.fxml","Стиратель памяти");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void blueLagoonWindow(ActionEvent actionEvent) {
        blueLagoon.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/blueLagoon.fxml","Голубая лагуна");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void blackRussianWIndow(ActionEvent actionEvent) {
        blackRussian.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/blackRussian.fxml","Чёрный русский");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void deadmanWindow(ActionEvent actionEvent) {
        deadMan.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/deadman.fxml","Благодарный мертвец");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void aperolWindow(ActionEvent actionEvent) {
        aperol.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/aperol.fxml","Апероль Шпритц");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void negroniWindow(ActionEvent actionEvent) {
        negroni.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/negroni.fxml","Негрони");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void otvertkaWIndow(ActionEvent actionEvent) {
        otvertka.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/otvertka.fxml","Отвёртка ");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void ginTonicWindow(ActionEvent actionEvent) {
        ginTonic.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/ginTonic.fxml","Джин Тоник");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void flyToMoonWindow(ActionEvent actionEvent) {
        flyToMoon.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/flyToMoon.fxml","Полёт на Луну");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void greenSpirit(ActionEvent actionEvent) {
        greenSpirit.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/greenSpirit.fxml","Виски Кола");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void shurupWindow(ActionEvent actionEvent) {
        shurup.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/shurup.fxml","Шуруповёрт");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void tequilaBoomWindow(ActionEvent actionEvent) {
        tequilaBoom.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/tequilaBoom.fxml","Текила Бум");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void longIslandWIndow(ActionEvent actionEvent) {
        longIsland.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/longIsland.fxml","Лонг Айленд");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void margaritaWIndow(ActionEvent actionEvent) {
        margarita.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/margarita.fxml","Маргарита");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void strawberryMargaritaWindow(ActionEvent actionEvent) {
        strawberryMargarita.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/strawberryMargarita.fxml","Клубничная Маргарита");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void daiquiriWindow(ActionEvent actionEvent) {
        daiquiri.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/daiquiri.fxml","Дайкири");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void pinaColadaWindow(ActionEvent actionEvent) {
        pinaColada.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/pinaColada.fxml","Пина Колада");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void husbandWindow(ActionEvent actionEvent) {
        husband.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/husband.fxml","День без мужа");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void meetingWindow(ActionEvent actionEvent) {
        meeting.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/meeting.fxml","Свидание");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void cherryLoveWindow(ActionEvent actionEvent) {
        cherryLove.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/cherryLove.fxml","Вишнёвая любовь");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void TequilaSunriseWindow(ActionEvent actionEvent) {
        TequilaSunrise.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/tequilaSunrise.fxml","Текила Санрайз");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void mohitoWIndow(ActionEvent actionEvent) {
        mohito.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/mohito.fxml","Мохито");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void cubaLibraWIndow(ActionEvent actionEvent) {
        cubaLibra.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/cubaLibra.fxml","Куба Либрэ");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void headshotWindow(ActionEvent actionEvent) {
        headShot.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/headshot.fxml","Выстрел в голову");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void hiroshimaWindow(ActionEvent actionEvent) {
        hiroshima.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/hiroshima.fxml","Хиросима");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void medusaWIndow(ActionEvent actionEvent) {
        medusa.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/medusa.fxml","Медуза");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void boyarskyWindow(ActionEvent actionEvent) {
        boyarsky.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/boyarsky.fxml","Боярский");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void b52Window(ActionEvent actionEvent) {
        b52.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/b52.fxml","Б52");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void rafaelloWindow(ActionEvent actionEvent) {
        rafaello.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/rafaello.fxml","Рафаэлло");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void beetleWindow(ActionEvent actionEvent) {
        beetle.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/beetle.fxml","Таракан");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void homelessWindow(ActionEvent actionEvent) {
        homeless.setOnAction(event -> {
            try {
                openNewScene("/cocktailsInfo/homeless.fxml","БОМЖ");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
