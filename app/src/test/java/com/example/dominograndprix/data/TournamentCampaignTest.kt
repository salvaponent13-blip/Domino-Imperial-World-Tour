package com.example.dominograndprix.data

import com.example.dominograndprix.model.GameMode
import org.junit.Assert.*
import org.junit.Test

class TournamentCampaignTest {

    @Test
    fun `campaign has exactly 7 venues and 35 progressive challenges`() {
        assertEquals(7, TournamentCampaign.VENUE_IDS.size)

        var totalChallenges = 0
        for (venueIndex in 0 until 7) {
            val challenges = TournamentCampaign.getChallengesForVenue(venueIndex)
            assertEquals(5, challenges.size)
            challenges.forEachIndexed { cIndex, challenge ->
                assertEquals(cIndex + 1, challenge.challengeNumber)
                assertTrue(challenge.targetScore > 0)
                assertNotNull(challenge.rivalName)
                assertNotNull(challenge.description)
            }
            totalChallenges += challenges.size
        }

        assertEquals(35, totalChallenges)
    }

    @Test
    fun `locations 1 to 4 are individual play and teams start in location 5 Miami`() {
        // Locations 0..3 (México, Cuba, Puerto Rico, New Orleans) must be individual
        for (venueIdx in 0..3) {
            val challenges = TournamentCampaign.getChallengesForVenue(venueIdx)
            challenges.forEach { challenge ->
                assertFalse("Venue $venueIdx should be individual: ${challenge.title}", challenge.isTeamGame)
            }
        }

        // Location 4 (Miami) introduces pairs 2v2 (Challenge 1 is team game)
        val miamiChallenges = TournamentCampaign.getChallengesForVenue(4)
        assertTrue(miamiChallenges[0].isTeamGame)
    }

    @Test
    fun `every venue final challenge has a unique luxury trophy`() {
        for (venueIdx in 0 until 7) {
            val finalChallenge = TournamentCampaign.getChallenge(venueIdx, 4)
            assertEquals(5, finalChallenge.challengeNumber)
            assertNotNull(finalChallenge.trophyTitle)
        }
    }

    @Test
    fun `getChallenge coerces out of bounds indices safely`() {
        val safeUnder = TournamentCampaign.getChallenge(-1, -1)
        assertEquals("mexico_1", safeUnder.id)

        val safeOver = TournamentCampaign.getChallenge(99, 99)
        assertEquals("tokyo_5", safeOver.id)
    }
}
