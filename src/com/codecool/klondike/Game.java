package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();







    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;

    private  List<Pile> addTableauToFoundation(List<Pile> firstList, List<Pile> secondList){
        List<Pile> result = FXCollections.observableArrayList();
        for(Pile pile : firstList){
            result.add(pile);
        }
        for(Pile pile: secondList){
            result.add(pile);
        }
        return result;
    }




    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK && card.getContainingPile().getTopCard().equals(card)) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();
        if (activePile.getPileType() == Pile.PileType.STOCK)
            return;
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;

        draggedCards.clear();
        draggedCards.add(card);

        card.getDropShadow().setRadius(20);
        card.getDropShadow().setOffsetX(10);
        card.getDropShadow().setOffsetY(10);

        card.toFront();
        card.setTranslateX(offsetX);
        card.setTranslateY(offsetY);
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        List<Pile> allPiles = addTableauToFoundation(foundationPiles,tableauPiles);
        Pile pile = getValidIntersectingPile(card, allPiles);
        //TODO
        if (pile != null) {
            handleValidMove(card, pile);
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }
    };

    public boolean isGameWon() {
        for(Pile pile : foundationPiles){
            if (!pile.getTopCard().getRank().equals(Card.Rank.KING)){
                return false;
                }
            }

        //TODO
        return true;
    }

    public Game() {
        deck = Card.createNewDeck();
        Collections.shuffle(deck);
        initPiles();
        dealCards();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        ObservableList<Card> cards = discardPile.getCards();
        for(Card card : cards){
            card.flip();
            stockPile.addCard(card);
        }
        discardPile.clear();
        //TODO
        System.out.println("Stock refilled from discard pile.");
    }

    public boolean isMoveValid(Card card, Pile destPile) {
        if(destPile == null){
            return false;
        }
        Pile.PileType pileType = destPile.getPileType();
        if(pileType.equals(Pile.PileType.FOUNDATION)){
            if(destPile.isEmpty() && card.getRank().equals(Card.Rank.ACE)){
                return true;
            }else if(destPile.isEmpty() && card.getRank() != Card.Rank.ACE){
                return false;
            }
            else if(pileType.equals(Pile.PileType.FOUNDATION) ){
                if(card.getSuit().equals(destPile.getTopCard().getSuit())){
                    Card.Rank topCardRank = destPile.getTopCard().getRank();
                    Card.Rank currentCardRank = card.getRank();
                    return topCardRank.getRankCode() == currentCardRank.getRankCode() - 1;
                }
            }
        } else if(pileType.equals(Pile.PileType.TABLEAU)) {
            if (destPile.isEmpty() && card.getRank().equals(Card.Rank.KING)) {
                return true;
            } else if (destPile.isEmpty() && card.getRank() != Card.Rank.KING) {
                return false;
            } else if (Card.isOppositeColor(destPile.getTopCard(),card)) {
                Card.Rank topCardRank = destPile.getTopCard().getRank();
                Card.Rank currentCardRank = card.getRank();
                return topCardRank.getRankCode() == currentCardRank.getRankCode() + 1;
            }

        }
        //TODO
        return false;
    }
    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    public void dealCards() {
        //TODO - itt kell megírni hogy alapból kerüljenek cardok a tableau pile-ba?

        for (int i = 0; i < 24; i++) {
            stockPile.addCard(deck.get(i));
            addMouseEventHandlers(deck.get(i));
            getChildren().add(deck.get(i));
        }

        
        int start = 24;
        for (int tableauIndex = 0; tableauIndex < tableauPiles.size(); tableauIndex++) {
            for (int i = start; i < start+tableauIndex+1; i++) {
                tableauPiles.get(tableauIndex).addCard(deck.get(i));
                addMouseEventHandlers(deck.get(i));
                getChildren().add(deck.get(i));

            }
            tableauPiles.get(tableauIndex).getTopCard().flip();
            start += tableauIndex+1;
        }
    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

}
