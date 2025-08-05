package io.github.natswarchuan.vmc.core.service;

import io.github.natswarchuan.vmc.core.dto.BaseDto;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.exception.VMCException;
import io.github.natswarchuan.vmc.core.persistence.service.SaveOptions;
import io.github.natswarchuan.vmc.core.query.builder.Paginator;
import io.github.natswarchuan.vmc.core.repository.VMCRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

/**
 * Một lớp trừu tượng cơ sở cung cấp triển khai mặc định cho các phương thức trong {@link
 * VMCService}.
 *
 * <p>Lớp này được thiết kế để giảm thiểu mã lặp (boilerplate code) trong các lớp service cụ thể. Nó
 * ủy quyền hầu hết các lời gọi đến một instance của {@link VMCRepository} được tiêm vào. Các lớp
 * service cụ thể nên kế thừa từ lớp này và chỉ cần cung cấp repository tương ứng.
 *
 * <p>Tất cả các phương thức trong lớp này đều được đánh dấu là {@code @Transactional} để đảm bảo
 * tính nhất quán của dữ liệu.
 *
 * @param <T> Kiểu của thực thể (entity).
 * @param <ID> Kiểu của khóa chính (primary key).
 * @param <R> Kiểu của repository, phải kế thừa từ {@code VMCRepository<T, ID>}.
 * @author NatswarChuan
 */
@Transactional(rollbackFor = Exception.class)
public abstract class AbstractVMCService<T extends Model, ID, R extends VMCRepository<T, ID>>
    implements VMCService<T, ID> {

  protected final R repository;

  protected AbstractVMCService(R repository) {
    this.repository = repository;
  }

  @Override
  public List<T> findAll() {
    return repository.findAll();
  }

  @Override
  public Paginator<T> findAll(int page, int perPage) {
    return repository.findAll(page, perPage);
  }

  @Override
  public Optional<T> findById(ID id) {
    return repository.findById(id);
  }

  @Override
  public <D extends BaseDto<T, D>> Optional<D> findByIdGetDto(ID id, Class<D> dtoClass) {
    return repository.findByIdGetDto(id, dtoClass);
  }

  @Override
  public <D extends BaseDto<T, D>> List<D> findAllGetDtos(Class<D> dtoClass) {
    return repository.findAllGetDtos(dtoClass);
  }

  @Override
  public <D extends BaseDto<T, D>> Paginator<D> findAllGetDtos(
      int page, int perPage, Class<D> dtoClass) {
    return repository.findAllGetDtos(page, perPage, dtoClass);
  }

  @Override
  public T create(T entity) {
    return repository.save(entity);
  }

  @Override
  public T create(T entity, SaveOptions options) {
    return repository.save(entity, options);
  }

  @Override
  public List<T> createAll(Iterable<T> entities) {
    return repository.saveAll(entities);
  }

  @Override
  public <D extends BaseDto<T, D>> D createFromDto(D dto) {
    return createFromDto(dto, new SaveOptions());
  }

  @Override
  public <D extends BaseDto<T, D>> D createFromDto(D dto, SaveOptions options) {
    T entity = dto.toEntity();
    T savedEntity = this.create(entity, options);
    return dto.toDto(savedEntity);
  }

  @Override
  public T update(ID id, T entity) {
    if (!repository.findById(id).isPresent()) {
      throw new VMCException(
          HttpStatus.NOT_FOUND, "Entity not found with id: " + id + " for update.");
    }
    return repository.save(entity);
  }

  @Override
  public T update(ID id, T entity, SaveOptions options) {
    if (!repository.findById(id).isPresent()) {
      throw new VMCException(
          HttpStatus.NOT_FOUND, "Entity not found with id: " + id + " for update.");
    }
    return repository.save(entity, options);
  }

  @Override
  public <D extends BaseDto<T, D>> D updateFromDto(ID id, D dto) {
    return updateFromDto(id, dto, new SaveOptions());
  }

  @Override
  public <D extends BaseDto<T, D>> D updateFromDto(ID id, D dto, SaveOptions options) {
    if (!repository.findById(id).isPresent()) {
      throw new VMCException(
          HttpStatus.NOT_FOUND, "Entity not found with id: " + id + " for update.");
    }
    T entityFromDto = dto.toEntity();
    entityFromDto.setPrimaryKey(id);
    T updatedEntity = this.save(entityFromDto, options);
    return dto.toDto(updatedEntity);
  }

  @Override
  public T save(T entity) {
    return repository.save(entity);
  }

  @Override
  public T save(T entity, SaveOptions options) {
    return repository.save(entity, options);
  }

  @Override
  public List<T> saveAll(Iterable<T> entities) {
    return repository.saveAll(entities);
  }

  @Override
  public <D extends BaseDto<T, D>> D saveFromDto(D dto) {
    return repository.saveDto(dto);
  }

  @Override
  public <D extends BaseDto<T, D>> D saveFromDto(D dto, SaveOptions options) {
    T entity = dto.toEntity();
    T savedEntity = repository.save(entity, options);
    return dto.toDto(savedEntity);
  }

  @Override
  public void deleteById(ID id) {
    T entity =
        this.findById(id)
            .orElseThrow(
                () -> new VMCException(HttpStatus.NOT_FOUND, "Entity not found with id: " + id));
    repository.delete(entity);
  }

  @Override
  public void delete(T entity) {
    repository.delete(entity);
  }

  @Override
  public long count() {
    return repository.count();
  }

  // --- TRIỂN KHAI CÁC PHƯƠNG THỨC MỚI (ĐÃ SỬA LỖI TYPE SAFETY) ---

  @Override
  public <I extends BaseDto<T, I>, O extends BaseDto<T, O>> O createFromDto(
      I requestDto, Class<O> responseDtoClass) {
    return createFromDto(requestDto, responseDtoClass, new SaveOptions());
  }

  @Override
  public <I extends BaseDto<T, I>, O extends BaseDto<T, O>> O createFromDto(
      I requestDto, Class<O> responseDtoClass, SaveOptions options) {
    T entity = requestDto.toEntity();
    T savedEntity = this.create(entity, options);
    try {
      O responseDto = responseDtoClass.getDeclaredConstructor().newInstance();
      return responseDto.toDto(savedEntity);
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Could not create response DTO instance.", e);
    }
  }

  @Override
  public <I extends BaseDto<T, I>, O extends BaseDto<T, O>> O updateFromDto(
      ID id, I requestDto, Class<O> responseDtoClass) {
    return updateFromDto(id, requestDto, responseDtoClass, new SaveOptions());
  }

  @Override
  public <I extends BaseDto<T, I>, O extends BaseDto<T, O>> O updateFromDto(
      ID id, I requestDto, Class<O> responseDtoClass, SaveOptions options) {
    if (!repository.findById(id).isPresent()) {
      throw new VMCException(
          HttpStatus.NOT_FOUND, "Entity not found with id: " + id + " for update.");
    }
    T entityFromDto = requestDto.toEntity();
    entityFromDto.setPrimaryKey(id);
    T updatedEntity = this.save(entityFromDto, options);
    try {
      O responseDto = responseDtoClass.getDeclaredConstructor().newInstance();
      return responseDto.toDto(updatedEntity);
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Could not create response DTO instance.", e);
    }
  }

  @Override
  public <I extends BaseDto<T, I>, O extends BaseDto<T, O>> O saveFromDto(
      I requestDto, Class<O> responseDtoClass) {
    return saveFromDto(requestDto, responseDtoClass, new SaveOptions());
  }

  @Override
  public <I extends BaseDto<T, I>, O extends BaseDto<T, O>> O saveFromDto(
      I requestDto, Class<O> responseDtoClass, SaveOptions options) {
    T entity = requestDto.toEntity();
    T savedEntity = this.save(entity, options);
    try {
      O responseDto = responseDtoClass.getDeclaredConstructor().newInstance();
      return responseDto.toDto(savedEntity);
    } catch (Exception e) {
      throw new VMCException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Could not create response DTO instance.", e);
    }
  }
}
