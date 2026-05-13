package com.apptism.repository;

import com.apptism.entity.RolUsuario;
import com.apptism.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link Usuario}.
 *
 * <p>Extiende {@link JpaRepository} para heredar las operaciones CRUD básicas.
 * Los métodos adicionales siguen la convención de nombres de Spring Data JPA,
 * que genera las consultas SQL automáticamente en tiempo de ejecución.</p>
 */

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su dirección de correo electrónico.
     *
     * @param email el correo a buscar
     * @return el usuario encontrado, o vacío si no existe ninguno con ese correo
     */

    Optional<Usuario> findByEmail(String email);

    /**
     * Busca un usuario que coincida exactamente con el email y la contraseña indicados.
     * Se usa para validar las credenciales en el inicio de sesión.
     *
     * @param email    el correo del usuario
     * @param password la contraseña en texto plano
     * @return el usuario si las credenciales son correctas, o vacío si no coincide
     */

    Optional<Usuario> findByEmailAndPassword(String email, String password);

    /**
     * Devuelve todos los usuarios que tienen el rol indicado.
     *
     * @param rol el rol por el que filtrar
     * @return lista de usuarios con ese rol; vacía si no hay ninguno
     */

    List<Usuario> findByRol(RolUsuario rol);
}
