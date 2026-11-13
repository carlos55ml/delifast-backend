package dev.carlosmoran.delifast.service;

import dev.carlosmoran.delifast.domain.Categoria;
import dev.carlosmoran.delifast.domain.Negocio;
import dev.carlosmoran.delifast.domain.Producto;
import dev.carlosmoran.delifast.dto.negocio.CategoriaCartaResponse;
import dev.carlosmoran.delifast.dto.negocio.DatosProductoRequest;
import dev.carlosmoran.delifast.dto.negocio.ProductoCartaResponse;
import dev.carlosmoran.delifast.exception.ConflictoException;
import dev.carlosmoran.delifast.exception.RecursoNoEncontradoException;
import dev.carlosmoran.delifast.exception.ValidacionException;
import dev.carlosmoran.delifast.repository.CategoriaRepository;
import dev.carlosmoran.delifast.repository.ProductoRepository;
import dev.carlosmoran.delifast.security.UsuarioAutenticado;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Carta del negocio: categorías y productos (N03, RF-17). Todo sobre el negocio del token. */
@Service
public class CartaService {

    private final NegocioActual negocioActual;
    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final Imagenes imagenes;

    public CartaService(NegocioActual negocioActual, CategoriaRepository categorias, ProductoRepository productos,
                        Imagenes imagenes) {
        this.negocioActual = negocioActual;
        this.categorias = categorias;
        this.productos = productos;
        this.imagenes = imagenes;
    }

    @Transactional(readOnly = true)
    public List<CategoriaCartaResponse> carta(UsuarioAutenticado usuario) {
        return categorias.findByNegocioIdOrderByOrdenAscIdAsc(negocioActual.id(usuario)).stream()
                .map(CategoriaCartaResponse::de)
                .toList();
    }

    /** Nueva categoría al final de la carta. */
    @Transactional
    public CategoriaCartaResponse crearCategoria(UsuarioAutenticado usuario, String nombre) {
        Negocio negocio = negocioActual.cargar(usuario);
        String limpio = nombre.trim();
        if (categorias.existsByNegocioIdAndNombreIgnoreCase(negocio.getId(), limpio)) {
            throw categoriaRepetida(limpio);
        }
        Categoria categoria = categorias.save(new Categoria(negocio, limpio, categorias.ultimoOrden(negocio.getId()) + 1));
        return CategoriaCartaResponse.de(categoria);
    }

    @Transactional
    public CategoriaCartaResponse renombrarCategoria(UsuarioAutenticado usuario, Long id, String nombre) {
        Long negocioId = negocioActual.id(usuario);
        Categoria categoria = categoria(id, negocioId);
        String limpio = nombre.trim();
        if (categorias.existsByNegocioIdAndNombreIgnoreCaseAndIdNot(negocioId, limpio, id)) {
            throw categoriaRepetida(limpio);
        }
        categoria.setNombre(limpio);
        return CategoriaCartaResponse.de(categoria);
    }

    /**
     * Borra una categoría sin productos. Los productos ya borrados (lógicamente) se van con
     * ella; los pedidos antiguos conservan nombre y precio copiados en sus líneas.
     */
    @Transactional
    public void borrarCategoria(UsuarioAutenticado usuario, Long id) {
        Categoria categoria = categoria(id, negocioActual.id(usuario));
        if (productos.existsByCategoriaIdAndActivoTrue(id)) {
            throw new ConflictoException("La categoría \"" + categoria.getNombre()
                    + "\" aún tiene productos: bórralos o muévelos antes");
        }
        categorias.delete(categoria);
    }

    @Transactional
    public ProductoCartaResponse crearProducto(UsuarioAutenticado usuario, DatosProductoRequest datos, MultipartFile foto) {
        Long negocioId = negocioActual.id(usuario);
        Categoria categoria = categoriaDelFormulario(datos.categoriaId(), negocioId);
        Producto producto = new Producto(categoria, datos.nombre().trim(), datos.precio());
        aplicar(producto, datos);
        if (foto != null && !foto.isEmpty()) {
            producto.setFotoUrl(imagenes.subir(foto, "foto", Imagenes.BUCKET_PRODUCTOS, negocioId));
        }
        return ProductoCartaResponse.de(productos.save(producto));
    }

    /** Edita el producto. Sin foto nueva se conserva la actual, salvo con quitarFoto. */
    @Transactional
    public ProductoCartaResponse editarProducto(UsuarioAutenticado usuario, Long id, DatosProductoRequest datos,
                                                MultipartFile foto) {
        Long negocioId = negocioActual.id(usuario);
        Producto producto = producto(id, negocioId);
        if (!producto.getCategoria().getId().equals(datos.categoriaId())) {
            producto.setCategoria(categoriaDelFormulario(datos.categoriaId(), negocioId));
        }
        producto.setNombre(datos.nombre().trim());
        producto.setPrecio(datos.precio());
        aplicar(producto, datos);
        String anterior = producto.getFotoUrl();
        if (foto != null && !foto.isEmpty()) {
            producto.setFotoUrl(imagenes.subir(foto, "foto", Imagenes.BUCKET_PRODUCTOS, negocioId));
            imagenes.borrarAlConfirmar(anterior);
        } else if (Boolean.TRUE.equals(datos.quitarFoto())) {
            producto.setFotoUrl(null);
            imagenes.borrarAlConfirmar(anterior);
        }
        return ProductoCartaResponse.de(producto);
    }

    /** Borrado lógico: sigue enlazado desde los pedidos antiguos. La foto sí se borra. */
    @Transactional
    public void borrarProducto(UsuarioAutenticado usuario, Long id) {
        Producto producto = producto(id, negocioActual.id(usuario));
        producto.setActivo(false);
        imagenes.borrarAlConfirmar(producto.getFotoUrl());
        producto.setFotoUrl(null);
    }

    @Transactional
    public ProductoCartaResponse cambiarDisponible(UsuarioAutenticado usuario, Long id, boolean disponible) {
        Producto producto = producto(id, negocioActual.id(usuario));
        if (disponible) {
            producto.marcarDisponible();
        } else {
            producto.marcarAgotado();
        }
        return ProductoCartaResponse.de(producto);
    }

    private static void aplicar(Producto producto, DatosProductoRequest datos) {
        producto.setDescripcion(datos.descripcion() == null || datos.descripcion().isBlank()
                ? null : datos.descripcion().trim());
        if (datos.disponible()) {
            producto.marcarDisponible();
        } else {
            producto.marcarAgotado();
        }
    }

    private Categoria categoria(Long id, Long negocioId) {
        return categorias.findByIdAndNegocioId(id, negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría " + id));
    }

    private Categoria categoriaDelFormulario(Long id, Long negocioId) {
        return categorias.findByIdAndNegocioId(id, negocioId)
                .orElseThrow(() -> new ValidacionException("categoriaId", "no existe en este negocio"));
    }

    private Producto producto(Long id, Long negocioId) {
        return productos.findByIdAndCategoriaNegocioIdAndActivoTrue(id, negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + id));
    }

    private static ConflictoException categoriaRepetida(String nombre) {
        return new ConflictoException("Ya hay una categoría \"" + nombre + "\" en la carta");
    }
}
