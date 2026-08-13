package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NearestNeighborSessionOrderTest {

    private static final List<String> DISPLAYED_ORDER =
        Arrays.asList("first", "second", "third", "fourth", "fifth");

    @Test
    public void theSessionImmediatelyBelowTheOneThatLeavesIsOfferedFirst() {
        List<String> nearestFirst = NearestNeighborSessionOrder.orderCandidatesNearestFirst(
            DISPLAYED_ORDER, DISPLAYED_ORDER.indexOf("third"),
            Arrays.asList("first", "second", "fourth", "fifth"));

        assertEquals("the row below the one that left must be offered before the top of the list",
            Arrays.asList("fourth", "fifth", "second", "first"), nearestFirst);
    }

    @Test
    public void theSessionImmediatelyAboveIsOfferedFirstWhenTheOneThatLeavesWasLast() {
        List<String> nearestFirst = NearestNeighborSessionOrder.orderCandidatesNearestFirst(
            DISPLAYED_ORDER, DISPLAYED_ORDER.indexOf("fifth"),
            Arrays.asList("first", "second", "third", "fourth"));

        assertEquals("with nothing below it the row above must be offered before the top of the list",
            Arrays.asList("fourth", "third", "second", "first"), nearestFirst);
    }

    @Test
    public void theNearestSurvivorBelowWinsWhenSeveralSessionsLeaveAtOnce() {
        List<String> nearestFirst = NearestNeighborSessionOrder.orderCandidatesNearestFirst(
            DISPLAYED_ORDER, DISPLAYED_ORDER.indexOf("second"),
            Arrays.asList("first", "fifth"));

        assertEquals("the surviving row furthest down still beats the one above when anything below survives",
            Arrays.asList("fifth", "first"), nearestFirst);
    }

    @Test
    public void theRowAboveIsTakenWhenEverythingBelowTheOneThatLeavesAlsoLeft() {
        List<String> nearestFirst = NearestNeighborSessionOrder.orderCandidatesNearestFirst(
            DISPLAYED_ORDER, DISPLAYED_ORDER.indexOf("fourth"),
            Arrays.asList("first", "second"));

        assertEquals("with every row below gone the nearest surviving row above must come first",
            Arrays.asList("second", "first"), nearestFirst);
    }

    @Test
    public void theCandidatesKeepTheirGivenOrderWhenThereIsNoPositionToAnchorOn() {
        List<String> candidates = Arrays.asList("second", "first", "fifth");

        List<String> nearestFirst = NearestNeighborSessionOrder.orderCandidatesNearestFirst(
            DISPLAYED_ORDER, NearestNeighborSessionOrder.NO_POSITION, candidates);

        assertEquals("an unknown anchor must not reorder the candidates", candidates, nearestFirst);
    }

    @Test
    public void aPositionBeyondTheDisplayedOrderLeavesTheCandidatesAlone() {
        List<String> candidates = Arrays.asList("second", "first");

        List<String> nearestFirst = NearestNeighborSessionOrder.orderCandidatesNearestFirst(
            DISPLAYED_ORDER, DISPLAYED_ORDER.size(), candidates);

        assertEquals("an anchor outside the order must not reorder the candidates", candidates, nearestFirst);
    }

    @Test
    public void aCandidateMissingFromTheDisplayedOrderIsOfferedLast() {
        List<String> nearestFirst = NearestNeighborSessionOrder.orderCandidatesNearestFirst(
            DISPLAYED_ORDER, DISPLAYED_ORDER.indexOf("second"),
            Arrays.asList("created-during-the-rebuild", "third", "first"));

        assertEquals("a session with no place in the order the owner saw must not be preferred over a neighbour",
            Arrays.asList("third", "first", "created-during-the-rebuild"), nearestFirst);
    }

    @Test
    public void noCandidatesGivesNothingToDisplay() {
        List<String> nearestFirst = NearestNeighborSessionOrder.orderCandidatesNearestFirst(
            DISPLAYED_ORDER, DISPLAYED_ORDER.indexOf("third"), Collections.<String>emptyList());

        assertEquals("no surviving session must produce no ordering rather than an invented one",
            Collections.<String>emptyList(), nearestFirst);
    }

    @Test
    public void theSessionThatLeavesIsNeverOfferedBackToItself() {
        List<String> nearestFirst = NearestNeighborSessionOrder.orderCandidatesNearestFirst(
            DISPLAYED_ORDER, DISPLAYED_ORDER.indexOf("third"),
            Arrays.asList("third", "fourth"));

        assertEquals("the row that left must not be selected as its own neighbour",
            Arrays.asList("fourth", "third"), nearestFirst);
    }
}
