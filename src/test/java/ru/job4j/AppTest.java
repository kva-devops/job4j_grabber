package ru.job4j;

import org.junit.Assert;
import org.junit.Test;

public class AppTest {

    @Test
    public void test() {
        Assert.assertEquals(1, new App().someLogic());
    }
}