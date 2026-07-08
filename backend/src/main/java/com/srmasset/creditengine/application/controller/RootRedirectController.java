package com.srmasset.creditengine.application.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Sem essa rota, GET / cai no resolvedor padrão de recursos estáticos do Spring
// (NoResourceFoundException, ver GlobalExceptionHandler) — não é um erro, mas
// também não orienta quem abre a API pela raiz. Redireciona para a documentação
// interativa, que é o ponto de entrada natural de uma API REST sem UI própria.
@Controller
public class RootRedirectController {

    @GetMapping("/")
    public String redirectToSwaggerUi() {
        return "redirect:/swagger-ui/index.html";
    }
}
