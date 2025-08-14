import{_ as l}from"./CodeBlock-BQZkd1iB.js";import{r as i,c as o,o as r,b as t,d as s,e as n}from"./index-RB8_H9km.js";const c={id:"dinh-nghia-entities",class:"mb-12 space-y-4 scroll-mt-20"},b={__name:"EntitiesSection",setup(m){const a=i(`
package io.github.natswarchuan.vmc.entity;

import io.github.natswarchuan.vmc.core.annotation.*;
import io.github.natswarchuan.vmc.core.entity.Model;
import java.time.LocalDateTime;
import lombok.*;

@Data
@VMCTable(name = "users")
public class User extends Model {

  @VMCPrimaryKey(name = "id")
  @VMCColumn(name = "id")
  private Long id;

  @VMCColumn(name = "name")
  private String name;

  @VMCColumn(name = "email")
  private String email;

  @VMCColumn(name = "created_at")
  private LocalDateTime createdAt;

  @VMCColumn(name = "updated_at")
  private LocalDateTime updatedAt;
    // ...
}
`);return(d,e)=>(r(),o("section",c,[e[0]||(e[0]=t("h2",{class:"text-3xl font-bold text-slate-900 border-b pb-2"},"3. Định nghĩa Entities",-1)),e[1]||(e[1]=t("p",null,"Entities là các lớp Java đại diện cho các bảng trong cơ sở dữ liệu. Tất cả các entity phải kế thừa từ lớp `Model`.",-1)),e[2]||(e[2]=t("div",{class:"bg-blue-100 border-l-4 border-blue-500 text-blue-700 p-4 rounded-md",role:"alert"},[t("p",{class:"font-bold"},"Annotation cơ bản"),t("ul",{class:"list-disc list-inside mt-2"},[t("li",null,[t("code",null,'@VMCTable(name = "...")'),n(": Đánh dấu lớp là entity và chỉ định tên bảng.")]),t("li",null,[t("code",null,"@VMCPrimaryKey"),n(": Đánh dấu trường là khóa chính.")]),t("li",null,[t("code",null,'@VMCColumn(name = "...")'),n(": Ánh xạ trường thành cột. Nếu không có `name`, tên cột sẽ được lấy từ tên trường.")])])],-1)),e[3]||(e[3]=t("h3",{class:"text-2xl font-semibold text-slate-800 pt-4"},"Ví dụ: Entity `User`",-1)),s(l,{language:"java",code:a.value},null,8,["code"])]))}};export{b as default};
