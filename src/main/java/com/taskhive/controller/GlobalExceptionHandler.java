package com.taskhive.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, RedirectAttributes redirectAttributes) {
        String message = ex.getMessage();

        if (message != null && message.startsWith("Access denied")) {
            redirectAttributes.addFlashAttribute("error", message);
            return "redirect:/workspaces";
        }

        redirectAttributes.addFlashAttribute("error", message != null ? message : "An unexpected error occurred");
        return "redirect:/workspaces";
    }
}
