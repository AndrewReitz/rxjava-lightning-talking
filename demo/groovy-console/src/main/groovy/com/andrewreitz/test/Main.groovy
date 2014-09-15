package com.andrewreitz.test

import retrofit.RestAdapter
import retrofit.http.GET
import retrofit.http.Query
import rx.Observable

class Main {
  static void main(args) {

    RestAdapter restAdapter = new RestAdapter.Builder()
            .setEndpoint("https://reddit.com")
            .build();

    def redditService = restAdapter.create(RedditService.class)

    redditService.getPosts()
            .flatMap({ reddit -> Observable.from(reddit.data.children as Iterable) })
            .map({ x -> x.data.title })
            .subscribe({ x -> println(x) })
  }
}

interface RedditService {
  @GET("/r/groovy.json") Observable<Object> getPosts()
  @GET("/r/groovy.json") Observable<Object> getPosts(@Query("after") String fullName,
                                                     @Query("count") String count)
}
