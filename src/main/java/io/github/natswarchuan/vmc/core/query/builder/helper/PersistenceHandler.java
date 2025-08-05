package io.github.natswarchuan.vmc.core.query.builder.helper;

import io.github.natswarchuan.vmc.core.dto.BaseDto;
import io.github.natswarchuan.vmc.core.entity.Model;
import io.github.natswarchuan.vmc.core.persistence.VMCPersistenceManager;
import io.github.natswarchuan.vmc.core.persistence.service.SaveOptions;
import io.github.natswarchuan.vmc.core.util.BeanUtil;

import java.util.List;

/**
 * Xử lý các thao tác lưu trữ (save) cho VMCQueryBuilder.
 * Đóng vai trò là một lớp tiện ích tĩnh để ủy quyền cho VMCPersistenceManager.
 */
public class PersistenceHandler {

  /**
   * Lưu một DTO, sử dụng SaveOptions mặc định.
   */
  public static <E extends Model, D extends BaseDto<E, D>> D saveDto(D dto) {
    return saveDto(dto, new SaveOptions());
  }

  /**
   * Lưu một DTO với SaveOptions tùy chỉnh.
   */
  public static <E extends Model, D extends BaseDto<E, D>> D saveDto(D dto, SaveOptions saveOptions) {
    VMCPersistenceManager persistenceManager = BeanUtil.getBean(VMCPersistenceManager.class);
    return persistenceManager.saveDto(dto, saveOptions);
  }

  /**
   * Lưu một danh sách các DTO, sử dụng SaveOptions mặc định.
   */
  public static <E extends Model, D extends BaseDto<E, D>> List<D> saveAllDtos(Iterable<D> dtos) {
    return saveAllDtos(dtos, new SaveOptions());
  }

  /**
   * Lưu một danh sách các DTO với SaveOptions tùy chỉnh.
   */
  public static <E extends Model, D extends BaseDto<E, D>> List<D> saveAllDtos(
      Iterable<D> dtos, SaveOptions saveOptions) {
    VMCPersistenceManager persistenceManager = BeanUtil.getBean(VMCPersistenceManager.class);
    return persistenceManager.saveAllDtos(dtos, saveOptions);
  }
}
