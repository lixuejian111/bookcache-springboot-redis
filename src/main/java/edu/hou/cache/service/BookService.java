package edu.hou.cache.service;

import edu.hou.cache.bean.Book;
import java.util.List; // 【新增】必须导入 List 包

public interface BookService {

    // 原有的方法：根据 ID 查询单个书籍
    Book getBookById(Integer id);

    // 【新增】获取所有书籍列表的方法
    List<Book> getAllBooks();
}