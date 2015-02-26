package activitylifecycle.schibsted.pl.activitylifecycle;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Jacek Kwiecień on 26.02.15.
 */
public class RetainedFragment extends Fragment {

    private OnNextExecutable onNextExecutable;
    private OnErrorExecutable onErrorExecutable;
    private OnCompletedExecutable onCompleteExecutable;

    public RetainedFragment() {
        super();
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("RetainedFragment", "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("RetainedFragment", "onCreateView");
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.inject(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i("RetainedFragment", "onViewCreated");

        if (onCompleteExecutable != null) {
            Log.i("RetainedFragment", "executing stacked onComplete");
            onCompleteExecutable.execute();
            onCompleteExecutable = null;
        }

        if (onNextExecutable != null) {
            Log.i("RetainedFragment", "executing stacked onNext");
            onNextExecutable.execute();
            onNextExecutable = null;
        }

        if (onErrorExecutable != null) {
            Log.i("RetainedFragment", "executing stacked onError");
            onErrorExecutable.execute();
            onErrorExecutable = null;
        }
    }

    @OnClick(R.id.button)
    protected void startThreadClicked() {
        Log.i("RetainedFragment", "OnClick");
        Observable.just(new Object())
                .cache()
                .delay(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {
                        Log.i("RetainedFragment", "onCompleted");
                        OnCompletedExecutable onComplete = new OnCompletedExecutable() {
                            @Override
                            public void execute() {
                                getParentActivity().setLabelText("onCompleted");
                            }
                        };

                        if (isAdded()) {
                            Log.i("RetainedFragment", "executing onComplete");
                            onComplete.execute();
                        } else {
                            Log.i("RetainedFragment", "stacking onComplete");
                            onCompleteExecutable = onComplete;
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("RetainedFragment", "onError");
                        OnErrorExecutable<Throwable> onError = new OnErrorExecutable<Throwable>(e) {
                            @Override
                            public void execute() {
                                getParentActivity().setLabelText("onError");
                            }
                        };

                        if (isAdded()) {
                            Log.i("RetainedFragment", "executing onError");
                            onError.execute();
                        } else {
                            Log.i("RetainedFragment", "stacking onError");
                            onErrorExecutable = onError;
                        }
                    }

                    @Override
                    public void onNext(Object o) {
                        Log.i("RetainedFragment", "onNext");
                        OnNextExecutable<Object> onNext = new OnNextExecutable<Object>(o) {
                            @Override
                            public void execute() {
                                getParentActivity().setLabelText("onNext");
                            }
                        };

                        if (isAdded()) {
                            Log.i("RetainedFragment", "executing onNext");
                            onNext.execute();
                        } else {
                            Log.i("RetainedFragment", "stacking onNext");
                            onNextExecutable = onNext;
                        }
                    }
                });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i("RetainedFragment", "onDetach");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.i("RetainedFragment", "onAttach");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i("RetainedFragment", "onDestroyView");
    }

    public MainActivity getParentActivity() {
        return (MainActivity) getActivity();
    }

    public static abstract class OnNextExecutable<T> {
        protected T object;

        public OnNextExecutable(T object) {
            this.object = object;
        }

        public abstract void execute();
    }

    public static abstract class OnCompletedExecutable {
        public abstract void execute();
    }

    public static abstract class OnErrorExecutable<T extends Throwable> {
        protected T throwable;

        public OnErrorExecutable(T throwable) {
            this.throwable = throwable;
        }

        public abstract void execute();
    }
}
