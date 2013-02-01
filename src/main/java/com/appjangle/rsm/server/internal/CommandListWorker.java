package com.appjangle.rsm.server.internal;

import io.nextweb.Link;
import io.nextweb.Node;
import io.nextweb.NodeList;
import io.nextweb.Session;
import io.nextweb.fn.Closure;
import io.nextweb.fn.ExceptionListener;
import io.nextweb.fn.ExceptionResult;
import io.nextweb.fn.IntegerResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import one.async.joiner.CallbackLatch;

import com.appjangle.rsm.client.commands.ComponentCommand;
import com.appjangle.rsm.server.RsmServerConfiguration;
import com.appjangle.rsm.server.internal.CommandWorker.CommandProcessedCallback;
import com.appjangle.rsm.server.internal.RsmServerComponent.RequestsProcessedCallback;

import de.mxro.server.ComponentContext;

public class CommandListWorker {

    private static boolean ENABLE_LOG = false;

    private final AtomicBoolean processing;

    private final List<NodeList> scheduled;
    private final Link commands;
    private final RsmServerConfiguration conf;
    private final Session session;
    private final ComponentContext context;

    public void addListToBeProcessed(final NodeList list) {
        scheduled.add(list);
    }

    public void startProcessingIfRequired() {
        processScheduledGuarded();
    }

    public boolean isWorking() {
        return processing.get();
    }

    protected void processScheduledGuarded() {
        synchronized (processing) {

            if (processing.get()) {
                if (ENABLE_LOG) {
                    System.out
                            .println(this
                                    + ": Skip processing because process already running.");
                }
                return;
            }

            processing.set(true);

        }

        processScheduled();
    }

    protected void processScheduled() {
        if (scheduled.size() == 0) {
            processing.set(false);
            if (ENABLE_LOG) {
                System.out.println(this + ": All pending operation cleared");
            }
            return;
        }

        final NodeList o = scheduled.get(0);
        scheduled.remove(0);

        final List<Object> values = new ArrayList<Object>(o.size());

        for (final Node n : o) {
            // message might have been deleted in the meanwhile
            if (n.exists()) {
                values.add(n.value());
            }
        }

        if (ENABLE_LOG) {

            System.out.println(this + ": Processing: " + values);
        }

        final List<Node> toBeProcessed = filter(o);

        if (toBeProcessed.size() == 0) {
            processScheduled();
            return;
        }

        processRequests(toBeProcessed, new RequestsProcessedCallback() {

            @Override
            public void onDone() {

                if (ENABLE_LOG) {
                    System.out.println(this + ": Completed processing: "
                            + values);
                }

                processScheduled();
            }
        });

    }

    private List<Node> filter(final NodeList o) {
        final ArrayList<Node> filteredList = new ArrayList<Node>(o.size());

        for (final Node child : o.nodes()) {

            if (child.exists()) {
                final Object value = child.value();

                if ((value instanceof ComponentCommand)) {
                    filteredList.add(child);
                }
            }

        }

        return filteredList;
    }

    private void processRequests(final List<Node> o,
            final RequestsProcessedCallback requestsProcessedCallback) {

        final CallbackLatch latch = new CallbackLatch(o.size()) {

            @Override
            public void onFailed(final Throwable t) {
                requestsProcessedCallback.onDone();
            }

            @Override
            public void onCompleted() {
                final IntegerResult clearVersionsRequest = commands
                        .clearVersions(10);

                // System.out.println("all done");

                clearVersionsRequest.catchExceptions(new ExceptionListener() {

                    @Override
                    public void onFailure(final ExceptionResult r) {
                        requestsProcessedCallback.onDone();
                    }
                });

                clearVersionsRequest.get(new Closure<Integer>() {

                    @Override
                    public void apply(final Integer o) {
                        // System.out.println("TRIGGER");
                        requestsProcessedCallback.onDone();
                    }
                });

            }
        };

        for (final Node child : o) {

            final Object value = child.value();

            final String childUri = child.uri();

            if (value instanceof ComponentCommand) {
                new CommandWorker(conf, commands, session, context).process(
                        child, value, childUri, new CommandProcessedCallback() {

                            @Override
                            public void onSuccess() {
                                latch.registerSuccess();
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                latch.registerFail(t);
                            }
                        });

                continue;
            }

            throw new IllegalStateException("Child of wrong type");

        }

    }

    public CommandListWorker(final Link commands,
            final RsmServerConfiguration conf, final Session session,
            final ComponentContext context) {
        super();
        this.commands = commands;
        this.conf = conf;
        this.session = session;
        this.context = context;
        this.processing = new AtomicBoolean(false);
        this.scheduled = Collections
                .synchronizedList(new LinkedList<NodeList>());
    }

}
