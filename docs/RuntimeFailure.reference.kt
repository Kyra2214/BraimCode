package your.app.package.runtime

/**
 * Falha classificada do runtime, com um código estável (útil para métricas,
 * exibição de diagnóstico ao usuário, e testes) e uma mensagem legível.
 */
class RuntimeFailure(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
