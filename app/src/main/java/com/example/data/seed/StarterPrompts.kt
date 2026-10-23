package com.example.data.seed

import com.example.data.local.PromptTemplateEntity

/**
 * Curated prompt templates installed on first launch.
 * {{placeholders}} are resolved by the Prompt Library before use.
 */
object StarterPrompts {

    private data class Seed(
        val title: String,
        val content: String,
        val category: String,
        val favorite: Boolean = false
    )

    private val seeds = listOf(
        Seed(
            title = "Explain Like I'm Five",
            content = "Explain {{topic}} like I am five years old. Use a real-world analogy, then give the precise technical version in under 150 words.",
            category = "Learning",
            favorite = true
        ),
        Seed(
            title = "Deep Research Brief",
            content = "Research {{topic}} thoroughly. Give me: 1) a concise executive summary, 2) five key findings with sources, 3) competing viewpoints, 4) open questions, 5) what to read next.",
            category = "Research"
        ),
        Seed(
            title = "Senior Code Review",
            content = "Act as a senior {{language}} engineer. Review the following code for correctness, security, performance, and readability. Provide a prioritized list of concrete fixes with patched snippets.\n\n```\n{{code}}\n```",
            category = "Coding",
            favorite = true
        ),
        Seed(
            title = "Debug Detective",
            content = "I am seeing this error in {{environment}}:\n\n{{error}}\n\nRelevant code:\n\n```\n{{code}}\n```\n\nList the three most likely root causes, the fastest diagnostic for each, and a minimal fix.",
            category = "Coding"
        ),
        Seed(
            title = "Polished Email",
            content = "Draft an email to {{recipient}} about {{subject}}. Tone: {{tone}}. Keep it under 180 words, include a clear subject line, one call to action, and no corporate jargon.",
            category = "Writing"
        ),
        Seed(
            title = "Meeting to Action Plan",
            content = "Turn these meeting notes into a clean action plan with owners, deadlines, risks, and decisions needed. Flag anything ambiguous.\n\n{{notes}}",
            category = "Work"
        ),
        Seed(
            title = "Decision Matrix",
            content = "Help me choose between {{option_a}} and {{option_b}} for {{goal}}. Build a weighted decision matrix, state your assumptions, and give a final recommendation with the strongest counter-argument.",
            category = "Analysis"
        ),
        Seed(
            title = "7-Day Learning Sprint",
            content = "Create a 7-day learning sprint for {{subject}}, assuming {{hours}} hours per day. Include daily outcomes, one project-based milestone, review checkpoints, and free resources.",
            category = "Learning"
        ),
        Seed(
            title = "Rewrite for Clarity",
            content = "Rewrite the following text for clarity and confidence. Preserve all facts, remove filler, and keep it under {{word_limit}} words.\n\n{{text}}",
            category = "Writing"
        ),
        Seed(
            title = "20 Fresh Ideas",
            content = "Brainstorm 20 non-obvious ideas for {{problem}}. Group them into safe, ambitious, and wild. For the best three, explain the first concrete step and the main risk.",
            category = "Creative"
        ),
        Seed(
            title = "Weekly Review Coach",
            content = "Run my weekly review. Ask me for wins, blockers, and energy levels one at a time, then produce priorities for next week and a realistic time-blocked plan.",
            category = "Daily",
            favorite = true
        ),
        Seed(
            title = "Interview Simulator",
            content = "Simulate a rigorous but supportive job interview for a {{role}} position at a {{company_type}} company. Ask one question at a time, score each answer out of 10, and give model answers.",
            category = "Career"
        ),
        Seed(
            title = "SQL Query Builder",
            content = "Write an efficient SQL query using this schema:\n\n{{schema}}\n\nGoal: {{goal}}. Explain the joins, indexes, and edge cases. Provide the dialect: {{dialect}}.",
            category = "Coding"
        ),
        Seed(
            title = "Travel Architect",
            content = "Design a {{days}}-day itinerary for {{destination}} with a budget of {{budget}}, starting from {{origin}}. Include neighborhoods, booking order, transit tips, local food, and one backup plan per day.",
            category = "Life"
        )
    )

    fun toEntities(): List<PromptTemplateEntity> = seeds.mapIndexed { index, seed ->
        PromptTemplateEntity(
            id = "starter_prompt_$index",
            title = seed.title,
            content = seed.content,
            category = seed.category,
            isFavorite = seed.favorite,
            createdAt = 0L,
            updatedAt = 0L
        )
    }
}
