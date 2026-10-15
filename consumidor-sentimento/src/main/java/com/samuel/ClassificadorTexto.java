package com.samuel;

import smile.classification.NaiveBayes;

import java.util.*;

/**
 * Classificador Naive Bayes multinomial usando a biblioteca Smile.
 * Converte texto em vetores de contagem de palavras (bag-of-words)
 * antes de entregar para o modelo, que trabalha apenas com double[].
 */
public class ClassificadorTexto {

    private final List<String> classes;
    private final Map<String, Integer> vocabulario = new LinkedHashMap<>();
    private final NaiveBayes modelo;

    public ClassificadorTexto(Map<String, List<String>> exemplosPorClasse) {
        this.classes = new ArrayList<>(exemplosPorClasse.keySet());

        // 1. Monta o vocabulário (palavra -> índice) a partir de todos os exemplos de treino
        for (List<String> frases : exemplosPorClasse.values()) {
            for (String frase : frases) {
                for (String palavra : tokenizar(frase)) {
                    vocabulario.putIfAbsent(palavra, vocabulario.size());
                }
            }
        }

        // 2. Converte cada frase de treino num vetor de contagem, junto com o índice da classe
        List<double[]> vetoresTreino = new ArrayList<>();
        List<Integer> rotulos = new ArrayList<>();

        for (int classeIdx = 0; classeIdx < classes.size(); classeIdx++) {
            for (String frase : exemplosPorClasse.get(classes.get(classeIdx))) {
                vetoresTreino.add(vetorizar(frase));
                rotulos.add(classeIdx);
            }
        }

        double[][] x = vetoresTreino.toArray(new double[0][]);
        int[] y = rotulos.stream().mapToInt(Integer::intValue).toArray();

        // 3. Treina o Naive Bayes multinomial (sigma = suavização de Laplace)
        this.modelo = NaiveBayes.fit(x, y, NaiveBayes.Model.MULTINOMIAL, classes.size(), 1.0);
    }

    public String classificar(String texto) {
        int predicao = modelo.predict(vetorizar(texto));
        return classes.get(predicao);
    }

    private double[] vetorizar(String texto) {
        double[] vetor = new double[vocabulario.size()];
        for (String palavra : tokenizar(texto)) {
            Integer indice = vocabulario.get(palavra);
            if (indice != null) vetor[indice]++; // palavra fora do vocabulário é ignorada
        }
        return vetor;
    }

    private List<String> tokenizar(String texto) {
        String normalizado = texto.toLowerCase().replaceAll("[^a-zà-ú0-9\\s]", " ");
        List<String> palavras = new ArrayList<>();
        for (String p : normalizado.split("\\s+")) {
            if (!p.isBlank()) palavras.add(p);
        }
        return palavras;
    }
}