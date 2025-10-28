package com.almis.awe.executor;

import com.almis.awe.thread.ContextAwareCallable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Created by pgarcia on 07/04/2017.
 */
public class ContextAwarePoolExecutor extends ThreadPoolTaskExecutor {

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return super.submit(new ContextAwareCallable<>(task, RequestContextHolder.getRequestAttributes(), SecurityContextHolder.getContext()));
	}

	@Override
	public <T> CompletableFuture<T> submitCompletable(Callable<T> task) {
		return super.submitCompletable(new ContextAwareCallable<>(task, RequestContextHolder.getRequestAttributes(), SecurityContextHolder.getContext()));
	}
}
