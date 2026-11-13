package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.exception.ValidacionException;
import dev.carlosmoran.delifast.supabase.AlmacenImagenes;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Imágenes de logos y productos (arquitectura 5.5). Comprueba el tipo real por los primeros
 * bytes (no se fía de la extensión ni del Content-Type del navegador) y el tamaño, y la sube a
 * Storage con un nombre nuevo. Va ligado a la transacción en curso: si la transacción falla se
 * borra la imagen subida, y la imagen que se sustituye solo se borra cuando se ha confirmado.
 */
@Component
public class Imagenes {

    public static final String BUCKET_LOGOS = "logos";
    public static final String BUCKET_PRODUCTOS = "productos";
    static final long TAMANO_MAXIMO = 2 * 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(Imagenes.class);

    private final AlmacenImagenes almacen;

    public Imagenes(AlmacenImagenes almacen) {
        this.almacen = almacen;
    }

    /** Tipo de imagen admitido, detectado por su firma. */
    enum Tipo {
        JPEG("image/jpeg", "jpg"), PNG("image/png", "png"), WEBP("image/webp", "webp");

        final String contentType;
        final String extension;

        Tipo(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }
    }

    /**
     * Valida y sube la imagen a {bucket}/{negocioId}/{uuid}.{ext}.
     *
     * @param campo nombre de la parte del formulario, para el mensaje de error ("logo", "foto")
     * @return URL pública
     */
    public String subir(MultipartFile fichero, String campo, String bucket, Long negocioId) {
        if (fichero.getSize() > TAMANO_MAXIMO) {
            throw new ValidacionException(campo, "no puede superar 2 MB");
        }
        byte[] contenido;
        try {
            contenido = fichero.getBytes();
        } catch (IOException e) {
            throw new ValidacionException(campo, "no se ha podido leer el fichero");
        }
        Tipo tipo = detectar(contenido);
        if (tipo == null) {
            throw new ValidacionException(campo, "tiene que ser una imagen JPEG, PNG o WebP");
        }
        String url = almacen.subir(bucket, negocioId + "/" + UUID.randomUUID() + "." + tipo.extension,
                contenido, tipo.contentType);
        alTerminar(false, url);
        return url;
    }

    /** Borra la imagen cuando se confirme la transacción (o ya, si no hay transacción). */
    public void borrarAlConfirmar(String url) {
        if (url != null) {
            alTerminar(true, url);
        }
    }

    private void alTerminar(boolean siSeConfirma, String url) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (siSeConfirma) {
                borrarSinFallar(url);
            }
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                boolean confirmada = estado == STATUS_COMMITTED;
                if (confirmada == siSeConfirma) {
                    borrarSinFallar(url);
                }
            }
        });
    }

    private void borrarSinFallar(String url) {
        try {
            almacen.borrar(url);
        } catch (RuntimeException e) {
            // Una imagen huérfana en Storage no rompe nada; se registra para limpiarla a mano.
            log.warn("No se pudo borrar la imagen {}", url, e);
        }
    }

    static Tipo detectar(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Tipo.JPEG;
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return Tipo.PNG;
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Tipo.WEBP;
        }
        return null;
    }
}
