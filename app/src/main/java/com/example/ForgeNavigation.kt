package com.example

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose keys for the 5 primary Forge UI destinations (Part 20 M13).
 */
@Serializable
sealed interface ForgeRoute {
    @Serializable
    data object Targets : ForgeRoute

    @Serializable
    data object WishBuilder : ForgeRoute

    @Serializable
    data object Lab : ForgeRoute

    @Serializable
    data object Files : ForgeRoute

    @Serializable
    data object Oracle : ForgeRoute
}
