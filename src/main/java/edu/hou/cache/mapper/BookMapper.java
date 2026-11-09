package edu.hou.cache.mapper;

import edu.hou.cache.bean.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BookMapper {

    // 查询所有书籍
    @Select("SELECT * FROM book")
    List<Book> findAll();

    // 根据ID查询（可选，用于后续扩展）
    @Select("SELECT * FROM book WHERE id = #{id}")
    Book findById(Integer id);
}