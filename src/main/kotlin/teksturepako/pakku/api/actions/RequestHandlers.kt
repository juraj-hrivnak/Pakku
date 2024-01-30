package teksturepako.pakku.api.actions

import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.projects.Project

fun interface WarningBlock
{
    suspend fun warning(message: String)
}

fun interface ErrorBlock
{
    suspend fun error(error: Error)
}

fun interface RetryBlock
{
    suspend fun retryWith(platform: Platform): Project?
}

fun interface SuccessBlock
{
    suspend fun success(project: Project, isRecommended: Boolean, ctx: RequestHandlers)
}

fun interface RemovalBlock
{
    suspend fun remove(project: Project, isRecommended: Boolean)
}