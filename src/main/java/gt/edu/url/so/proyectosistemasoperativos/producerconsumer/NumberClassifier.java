package gt.edu.url.so.proyectosistemasoperativos.producerconsumer;

public class NumberClassifier {

    public static TipoNumero clasificar(int n) {
        if (esPrimo(n)) return TipoNumero.PRIMO;
        if (n % 2 == 0) return TipoNumero.PAR;
        return TipoNumero.IMPAR;
    }

    private static boolean esPrimo(int n) {
        if (n < 2) return false;
        for (int i = 2; i <= Math.sqrt(n); i++) {
            if (n % i == 0) return false;
        }
        return true;
    }
}
