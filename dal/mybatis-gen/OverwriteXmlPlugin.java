import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import java.lang.reflect.Field;
import java.util.List;

/**
 * 用于解决 Mybatis generator mapper文件重新生成不会覆盖原文件的 bug
 * 用法: 把class生成脚本同级目录，并在generatorConfig.xml <context>下添加 <plugin type="OverwriteXmlPlugin"/> 节点
 * by coderzc
 */
public class OverwriteXmlPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> list) {
        return true;
    }

    @Override
    public boolean sqlMapGenerated(GeneratedXmlFile sqlMap, IntrospectedTable introspectedTable) {
        try {
            Field field = sqlMap.getClass().getDeclaredField("isMergeable");
            field.setAccessible(true);
            field.setBoolean(sqlMap, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.sqlMapGenerated(sqlMap, introspectedTable);
    }
}