package com.truckify.app.firebase

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.truckify.app.BuildConfig

import com.truckify.app.models.Driver
import com.truckify.app.models.Shipment

object GeminiManager {
    private val API_KEY = BuildConfig.GEMINI_API_KEY
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = API_KEY
    )

    suspend fun getRecommendedDriver(shipment: Shipment, candidates: List<Driver>): String = withContext(Dispatchers.IO) {
        val candidatesList = candidates.joinToString("\n") { 
            "Name: ${it.name}, Truck: ${it.truckType}, Capacity: ${it.capacity}, Rating: ${it.rating}, Trust Score: ${it.trustScore}%, Trips: ${it.totalTrips}, Experience: ${it.experienceYears}y"
        }
        
        val prompt = """
            You are the Truckify AI Logistics Engine. 
            Analyze the following shipment and available driver candidates to find the best match.
            
            Shipment Details:
            From: ${shipment.pickupAddress}
            To: ${shipment.destinationAddress}
            Weight: ${shipment.weight}
            Price: ${shipment.price}
            
            Available Drivers:
            $candidatesList
            
            Task:
            1. Recommend the top 1 driver based on their Trust Score, rating, and truck capacity.
            2. Explain WHY they are the best fit.
            3. Estimate the 'Acceptance Probability' for this driver.
            
            Return the result in a concise summary.
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            response.text ?: "Could not determine best match."
        } catch (e: Exception) {
            "Analysis failed: ${e.message}"
        }
    }

    suspend fun getCostPrediction(
        pickup: String,
        destination: String,
        truckType: String,
        weight: String
    ): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are a logistics cost estimator for Truckify.
            Predict the following for a trip from $pickup to $destination:
            1. Estimated distance in KM.
            2. Estimated Fuel Consumption (based on $truckType and $weight load).
            3. Estimated Fuel Cost (current diesel price approx ₹90/L).
            4. Estimated Toll Charges.
            5. Suggested Delivery Price for profitability.
            
            Format the response as a simple list.
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            response.text ?: "Prediction unavailable."
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun getChatResponse(userMessage: String, userRole: String): String = withContext(Dispatchers.IO) {
        val chat = model.startChat(
            history = listOf(
                content("user") { text("You are the AI Logistics Engine for Truckify. Your goal is to optimize matching between Vendors and Drivers. You analyze: 1. Nearest drivers using GPS. 2. Cheapest routes by analyzing tolls and fuel. 3. Best trucks for specific loads. 4. Minimizing 'Empty Miles' by finding return loads along the same route. User role: $userRole") },
                content("model") { text("Truckify AI Logistics Engine online. Ready to optimize your fleet and route efficiency.") }
            )
        )

        try {
            val response = chat.sendMessage(userMessage)
            response.text ?: "I'm sorry, I couldn't process that request."
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: "Connection failed"}. Please check your API key and internet."
        }
    }

    suspend fun getOptimizedRoute(pickup: String, stops: List<String>, destination: String): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are a logistics route optimizer for Truckify.
            Optimize the following delivery sequence:
            Start: $pickup
            Stops: ${stops.joinToString(", ")}
            End: $destination
            
            Task:
            1. Reorder the 'Stops' to minimize total travel time and distance.
            2. Explain WHY this sequence is optimal (mention major highways or congestion avoidance if applicable).
            
            Return the optimized list and a brief explanation.
        """.trimIndent()
        try {
            val response = model.generateContent(prompt)
            response.text ?: "Could not optimize route."
        } catch (e: Exception) {
            "Optimization failed: ${e.message}"
        }
    }
}
