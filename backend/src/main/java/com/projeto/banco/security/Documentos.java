package com.projeto.banco.security;

/** Validação e máscara de CPF/CNPJ. */
public final class Documentos {
    private Documentos() {}

    public static String somenteDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    public static boolean cpfValido(String d) {
        if (d == null || !d.matches("\\d{11}") || d.chars().distinct().count() == 1) return false;
        return dv(d, 9, 10) == d.charAt(9) - '0' && dv(d, 10, 11) == d.charAt(10) - '0';
    }

    private static int dv(String d, int len, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < len; i++) soma += (d.charAt(i) - '0') * (pesoInicial - i);
        int r = (soma * 10) % 11;
        return r == 10 ? 0 : r;
    }

    public static boolean cnpjValido(String d) {
        if (d == null || !d.matches("\\d{14}") || d.chars().distinct().count() == 1) return false;
        int[] p1 = { 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
        int[] p2 = { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
        return dvCnpj(d, p1) == d.charAt(12) - '0' && dvCnpj(d, p2) == d.charAt(13) - '0';
    }

    private static int dvCnpj(String d, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) soma += (d.charAt(i) - '0') * pesos[i];
        int r = soma % 11;
        return r < 2 ? 0 : 11 - r;
    }

    /** 12345678909 -> ***.456.789-** */
    public static String mascararCpf(String d) {
        return "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
    }

    /** 12345678000195 -> **.345.678/0001-** */
    public static String mascararCnpj(String d) {
        return "**." + d.substring(2, 5) + "." + d.substring(5, 8) + "/" + d.substring(8, 12) + "-**";
    }
}
