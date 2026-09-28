package soys.soysmonthlycard.web;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * 主线程同步工具：注解式 API 在 HTTP worker 线程执行，而绝大多数 Bukkit API
 * （玩家背包、发放奖励、指令执行等）只能在主线程调用。
 *
 * <p>本工具通过 {@code FutureTask} + {@code runTask} 把操作切回主线程并阻塞等待结果，
 * 模式与框架 {@code ApiRequestContext#getSyncPlayer()} 一致。</p>
 */
public final class Sync {

    private Sync() {
    }

    /**
     * 在主线程同步执行并返回结果。
     * 若当前已在主线程则直接执行，避免重复调度。
     */
    public static <T> T run(Plugin plugin, Callable<T> callable) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return callable.call();
            } catch (Exception e) {
                throw wrap(e);
            }
        }
        FutureTask<T> task = new FutureTask<>(callable);
        Bukkit.getScheduler().runTask(plugin, task);
        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待主线程操作被中断", e);
        } catch (ExecutionException e) {
            throw wrap(e.getCause() != null ? e.getCause() : e);
        }
    }

    /**
     * 在主线程同步执行无返回值操作。
     */
    public static void run(Plugin plugin, Runnable runnable) {
        run(plugin, () -> {
            runnable.run();
            return null;
        });
    }

    private static RuntimeException wrap(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
    }
}
