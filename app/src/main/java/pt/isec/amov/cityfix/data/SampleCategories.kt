package pt.isec.amov.cityfix.data

import pt.isec.amov.cityfix.model.Category

/**
 * Phase-1 categories (no Firebase). Ids match SampleTickets; "graffiti" is inactive to show the
 * I.3.E rule (citizens only report on active categories).
 */
object SampleCategories {
    val all: List<Category> = listOf(
        Category(id = "pavement", name = "Pavement"),
        Category(id = "lighting", name = "Public lighting"),
        Category(id = "furniture", name = "Urban furniture"),
        Category(id = "graffiti", name = "Graffiti", active = false),
    )
}
