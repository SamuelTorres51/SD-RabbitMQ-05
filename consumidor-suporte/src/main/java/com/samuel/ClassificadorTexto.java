package com.samuel;

import smile.classification.DiscreteNaiveBayes;

import java.util.*;


public class ClassificadorTexto {

    private final List<String> classes;
    private final Map<String, Integer> vocabulario = new LinkedHashMap<>();
    private final DiscreteNaiveBayes modelo;

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

        int k = classes.size();
        int p = vocabulario.size();

        // 2. Converte cada frase de treino num vetor de contagem de palavras (int[]),
        //    junto com o índice da classe correspondente
        List<int[]> vetoresTreino = new ArrayList<>();
        List<Integer> rotulos = new ArrayList<>();

        for (int classeIdx = 0; classeIdx < k; classeIdx++) {
            for (String frase : exemplosPorClasse.get(classes.get(classeIdx))) {
                vetoresTreino.add(vetorizar(frase));
                rotulos.add(classeIdx);
            }
        }

        int[][] x = vetoresTreino.toArray(new int[0][]);
        int[] y = rotulos.stream().mapToInt(Integer::intValue).toArray();

        // 3. Cria o classificador vazio (k classes, p features) e treina em lote
        this.modelo = new DiscreteNaiveBayes(DiscreteNaiveBayes.Model.MULTINOMIAL, k, p);
        this.modelo.update(x, y);
    }

    public String classificar(String texto) {
        int predicao = modelo.predict(vetorizar(texto));
        if (predicao < 0 || predicao >= classes.size()) {
            // Frase sem nenhuma palavra reconhecida pelo vocabulário de treino
            return "indefinido";
        }
        return classes.get(predicao);
    }

    private int[] vetorizar(String texto) {
        int[] vetor = new int[vocabulario.size()];
        for (String palavra : tokenizar(texto)) {
            Integer indice = vocabulario.get(palavra);
            if (indice != null) vetor[indice]++;
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