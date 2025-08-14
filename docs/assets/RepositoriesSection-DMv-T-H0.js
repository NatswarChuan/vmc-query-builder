import{_ as r}from"./CodeBlock-BQZkd1iB.js";import{r as s,c as i,o as a,b as t,d as n}from"./index-RB8_H9km.js";const c={id:"tao-repositories",class:"mb-12 space-y-4 scroll-mt-20"},y={__name:"RepositoriesSection",setup(p){const e=s(`
package io.github.natswarchuan.vmc.repository;

import io.github.natswarchuan.vmc.core.repository.VMCRepository;
import io.github.natswarchuan.vmc.entity.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends VMCRepository<User, Long > {
  List<User> findByNameContaining(String name);
}
`);return(l,o)=>(a(),i("section",c,[o[0]||(o[0]=t("h2",{class:"text-3xl font-bold text-slate-900 border-b pb-2"},"5. Tạo Repositories",-1)),o[1]||(o[1]=t("p",null,"Repository là các interface chịu trách nhiệm truy cập dữ liệu, kế thừa từ `VMCRepository<T, ID>`. `VMCRepository` đã cung cấp sẵn các phương thức CRUD cơ bản.",-1)),n(r,{language:"java",code:e.value},null,8,["code"])]))}};export{y as default};
