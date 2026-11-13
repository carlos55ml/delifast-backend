package dev.carlosmoran.delifast.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.carlosmoran.delifast.exception.ValidacionException;
import dev.carlosmoran.delifast.supabase.AlmacenImagenes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ImagenesTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    private final AlmacenImagenes almacen = mock(AlmacenImagenes.class);
    private final Imagenes imagenes = new Imagenes(almacen);

    @AfterEach
    void limpiar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void detectaElTipoPorLaFirma() {
        assertThat(Imagenes.detectar(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})).isEqualTo(Imagenes.Tipo.JPEG);
        assertThat(Imagenes.detectar(PNG)).isEqualTo(Imagenes.Tipo.PNG);
        assertThat(Imagenes.detectar(WEBP)).isEqualTo(Imagenes.Tipo.WEBP);
        assertThat(Imagenes.detectar("GIF89a".getBytes())).isNull();
        assertThat(Imagenes.detectar(new byte[0])).isNull();
    }

    @Test
    void subeConElTipoRealAunqueLaExtensionMienta() {
        when(almacen.subir(any(), any(), any(), any())).thenReturn("https://url");

        String url = imagenes.subir(new MockMultipartFile("foto", "foto.jpg", "image/jpeg", PNG), "foto", "productos", 7L);

        assertThat(url).isEqualTo("https://url");
        verify(almacen).subir(eq("productos"), startsWith("7/"), eq(PNG), eq("image/png"));
    }

    @Test
    void rechazaLoQueNoEsImagen() {
        assertThatThrownBy(() -> imagenes.subir(new MockMultipartFile("foto", "x.png", "image/png", "<svg/>".getBytes()),
                "foto", "productos", 7L))
                .isInstanceOf(ValidacionException.class)
                .hasFieldOrPropertyWithValue("errores", java.util.List.of("foto: tiene que ser una imagen JPEG, PNG o WebP"));
        verify(almacen, never()).subir(any(), any(), any(), any());
    }

    @Test
    void rechazaMasDeDosMegas() {
        byte[] grande = new byte[(int) Imagenes.TAMANO_MAXIMO + 1];
        assertThatThrownBy(() -> imagenes.subir(new MockMultipartFile("logo", "l.png", "image/png", grande),
                "logo", "logos", 7L))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void siLaTransaccionFallaBorraLaImagenSubida() {
        when(almacen.subir(any(), any(), any(), any())).thenReturn("https://nueva");
        TransactionSynchronizationManager.initSynchronization();

        imagenes.subir(new MockMultipartFile("foto", "f.png", "image/png", PNG), "foto", "productos", 7L);
        imagenes.borrarAlConfirmar("https://vieja");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(almacen).borrar("https://nueva");
        verify(almacen, never()).borrar("https://vieja");
    }

    @Test
    void siSeConfirmaBorraLaAnterior() {
        when(almacen.subir(any(), any(), any(), any())).thenReturn("https://nueva");
        TransactionSynchronizationManager.initSynchronization();

        imagenes.subir(new MockMultipartFile("foto", "f.png", "image/png", PNG), "foto", "productos", 7L);
        imagenes.borrarAlConfirmar("https://vieja");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

        verify(almacen).borrar("https://vieja");
        verify(almacen, never()).borrar("https://nueva");
    }

    @Test
    void unFalloAlBorrarNoRompeNada() {
        org.mockito.Mockito.doThrow(new RuntimeException("caído")).when(almacen).borrar(anyString());

        imagenes.borrarAlConfirmar("https://vieja");
    }
}
