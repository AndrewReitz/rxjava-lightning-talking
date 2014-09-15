package com.andrewreitz.test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import model.Child;
import model.Data;
import model.Listing;
import model.Reddit;
import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

public class Main {

  public static void main(String[] args) {

    RestAdapter restAdapter = new RestAdapter.Builder()
        .setEndpoint("https://reddit.com")
        .setLogLevel(RestAdapter.LogLevel.BASIC)
        .build();

    final RedditService service = restAdapter.create(RedditService.class);

    service.getPosts()
        .map(Reddit::getData)
        .map(listing -> {
          List<Child> children = Observable.from(listing.getChildren())
              .filter(child -> child.getData().getTitle().startsWith("On this day:"))
              .toList()
              .toBlocking()
              .first();

          final int[] size = { children.size() };

          List<Observable<Child>> observables = new LinkedList<>();
          observables.add(Observable.from(children));
          for (int i = 25; size[0] < 10; i += 25) {
            Observable<Child> childObservable = observables.get(observables.size() - 1);
            Child last = childObservable
                .doOnNext(child -> size[0]++)
                .takeLast(1)
                .toBlocking()
                .first();

            Observable<Child> moreChildren = service.getPosts(last.getData().getName(), i)
                .flatMap(reddit -> Observable.from(reddit.getData().getChildren()))
                .filter(child -> child.getData().getTitle().startsWith("On this day:"));

            observables.add(moreChildren);
          }

          return observables;

        })
        .flatMap(Observable::from)
        .flatMap(childObservable -> childObservable)
        .map(Child::getData)
        .map(Data::getTitle)
        .subscribe(System.out::println);

  }
}

interface RedditService {
  @GET("/r/gratefuldead.json") Observable<Reddit> getPosts();

  @GET("/r/gratefuldead.json") Observable<Reddit> getPosts(@Query("after") String fullName,
                                                           @Query("count") int count);
}
