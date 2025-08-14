import{_ as t}from"./CodeBlock-BI-7EVGF.js";import{r as i,c as l,o,b as r,d as n}from"./index-BVd1TkhY.js";const s={id:"query-builder",class:"mb-12 space-y-4 scroll-mt-20"},p={__name:"QueryBuilderSection",setup(a){const u=i(`
import io.github.natswarchuan.vmc.core.query.builder.VMCQueryBuilder;
import io.github.natswarchuan.vmc.core.query.enums.*;

// ... trong UserService.java
  public List<User> findWithQueryBuilder(String roleName) {
    return VMCQueryBuilder.from(User.class, "u")
        .join(VMCSqlJoinType.INNER_JOIN, "user_roles", "ur", "u.id", "=", "ur.user_id")
        .join(VMCSqlJoinType.INNER_JOIN, "roles", "r", "ur.role_id", "=", "r.id")
        .where("r.name", VMCSqlOperator.LIKE, roleName)
        .get();
}
`);return(d,e)=>(o(),l("section",s,[e[0]||(e[0]=r("h2",{class:"text-3xl font-bold text-slate-900 border-b pb-2"},"7. Sử dụng VMCQueryBuilder",-1)),e[1]||(e[1]=r("p",null,"`VMCQueryBuilder` là một công cụ mạnh mẽ để xây dựng các câu lệnh `SELECT` động. Truy vấn phải được bắt đầu bằng phương thức tĩnh `VMCQueryBuilder.from(YourEntity.class)`.",-1)),e[2]||(e[2]=r("p",null,"Ví dụ thực tế từ `UserService.findWithQueryBuilder`:",-1)),n(t,{language:"java",code:u.value},null,8,["code"])]))}};export{p as default};
