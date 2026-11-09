package edu.hou.cache.service.impl;
import edu.hou.cache.bean.Book;
import edu.hou.cache.mapper.BookMapper;
import edu.hou.cache.service.BookService;
import edu.hou.cache.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
@Service
public class BookServiceImpl implements BookService {
    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private CacheService cacheService;
    // 缓存Key常量，统一管理，方便后期修改
    private static final String BOOK_SINGLE_KEY_PREFIX = "book:";
    private static final String BOOK_ALL_LIST_KEY = "book:list:all";
    /**
     * 根据ID查询单本图书，先查缓存，无数据再查库并缓存
     * @param id 图书ID
     * @return 图书对象
     */
    @Override
    public Book getBookById(Integer id) {
        // 拼接单本书缓存key
        String cacheKey = BOOK_SINGLE_KEY_PREFIX + id;
        // 读取缓存
        Object cacheData = cacheService.getFromCache(cacheKey);
        // 缓存命中，直接返回
        if (cacheData instanceof Book) {
            System.out.printf(">>> [命中缓存] 读取Redis图书，ID=%d%n", id);
            return (Book) cacheData;
        }
        System.out.printf(">>> [缓存失效] 查询MySQL图书，ID=%d%n", id);
        Book book = bookMapper.findById(id);
        if (book != null) {
            System.out.printf(">>> [缓存预热] 写入Redis图书，ID=%d%n", id);
            cacheService.saveToCache(cacheKey, book);
        } else {
            System.out.printf(">>> [无数据] 数据库不存在ID=%d的图书%n", id);
        }
        return book;
    }
    @Override
    public List<Book> getAllBooks() {
        String cacheKey = BOOK_ALL_LIST_KEY;
        Object cacheData = cacheService.getFromCache(cacheKey);
        if (cacheData instanceof List<?>) {
            System.out.println(">>> [列表命中缓存] 直接读取Redis全部图书数据");
            return (List<Book>) cacheData;
        }
        System.out.println(">>> [列表缓存失效] 查询MySQL全部图书");
        List<Book> bookList = bookMapper.findAll();

        if (!CollectionUtils.isEmpty(bookList)) {
            System.out.println(">>> [列表缓存预热] 将全部图书写入Redis");
            cacheService.saveToCache(cacheKey, bookList);
        } else {
            System.out.println(">>> [无数据] 数据库图书列表为空");
        }
        return bookList;
    }
}