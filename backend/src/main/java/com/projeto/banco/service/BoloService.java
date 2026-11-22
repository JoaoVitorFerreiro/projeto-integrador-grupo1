package com.projeto.banco.service;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
public class BoloService {

    private static final Pattern VALIDO = Pattern.compile(
            "^bolo-\\d-andar(?:es)?-(coracao|quadrado|redondo)-.+-(branca|chocolate|naked)(-flores|-frutas|-lisa)?\\.webp$");

    /** Nomes (sem extensão e sem sufixo de decoração) dos modelos em classpath:/static/bolos/*.webp. */
    public List<String> listarModelos() {
        try {
            Resource[] recursos = new PathMatchingResourcePatternResolver().getResources("classpath:/static/bolos/*.webp");
            return java.util.Arrays.stream(recursos)
                    .map(Resource::getFilename)
                    .filter(n -> n != null && VALIDO.matcher(n).matches())
                    .map(n -> n.substring(0, n.length() - 5).replaceAll("-(flores|frutas|lisa)$", ""))
                    .distinct().sorted().toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
