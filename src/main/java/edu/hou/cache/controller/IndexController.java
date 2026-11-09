package edu.hou.cache.controller;
import edu.hou.cache.bean.Book;
import edu.hou.cache.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.ArrayList;
import java.util.List;
@Controller
public class IndexController {
    @Autowired
    private BookService bookService;
    @GetMapping("/")
    public String index() {
        return "index";
    }
    @GetMapping("/api/books")
    @ResponseBody
    public List<Book> listBooks() {
        try {
            System.out.println(">>> 已收到查询所有书籍的请求");
            List<Book> books = bookService.getAllBooks();
            System.out.println(">>> 查询成功，数量: " + (books == null ? 0 : books.size()));
            return books;
        } catch (Exception e) {
            System.err.println(">>> 发生严重错误: " + e.getMessage());
            e.printStackTrace(); // 在控制台打印详细报错信息
            return new ArrayList<>(); // 出错时返回空列表，防止前端崩溃
        }
    }

    @GetMapping("/book/{id}")
    @ResponseBody
    public Book getBook(@PathVariable Integer id) {
        return bookService.getBookById(id);
    }
}