package biz.dealnote.messenger.mvp.presenter

import android.content.Context
import android.os.Bundle
import biz.dealnote.messenger.R
import biz.dealnote.messenger.domain.IMessagesRepository
import biz.dealnote.messenger.domain.Repository.messages
import biz.dealnote.messenger.model.Message
import biz.dealnote.messenger.model.Peer
import biz.dealnote.messenger.mvp.presenter.base.PlaceSupportPresenter
import biz.dealnote.messenger.mvp.view.ILocalJsonToChatView
import biz.dealnote.messenger.util.Pair
import biz.dealnote.messenger.util.PersistentLogger
import biz.dealnote.messenger.util.RxUtils
import biz.dealnote.messenger.util.Utils
import biz.dealnote.mvp.reflect.OnGuiCreated
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class LocalJsonToChatPresenter(accountId: Int, private val context: Context, savedInstanceState: Bundle?) : PlaceSupportPresenter<ILocalJsonToChatView>(accountId, savedInstanceState) {
    private val mPost: ArrayList<Message> = ArrayList()
    private val mCached: ArrayList<Message> = ArrayList()
    private var isAttachments: Boolean
    private var isMy: Boolean
    private var peer: Peer
    private val fInteractor: IMessagesRepository = messages
    private val actualDataDisposable = CompositeDisposable()
    override fun onGuiCreated(viewHost: ILocalJsonToChatView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mPost)
    }

    fun togleAttachment(isMyTogle: Boolean) {
        if (!isMyTogle) {
            isAttachments = !isAttachments
        } else {
            isMy = !isMy
        }
        mPost.clear()
        if (!isAttachments) {
            if (!isMy) {
                mPost.addAll(mCached)
            } else {
                for (i in mCached) {
                    if (i.isOut) {
                        mPost.add(i)
                    }
                }
            }
        } else {
            for (i in mCached) {
                if (i.isHasAttachments || i.forwardMessagesCount > 0) {
                    if (isMy && i.isOut) {
                        mPost.add(i)
                    } else if (!isMy) {
                        mPost.add(i)
                    }
                }
            }
        }
        resolveToolbar()
        view?.notifyDataSetChanged()
        resolveRefreshingView(false)
    }

    private fun loadActualData() {
        resolveRefreshingView(true)
        val accountId = super.getAccountId()
        actualDataDisposable.add(fInteractor.getMessagesFromLocalJSon(accountId, context)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe({ data: Pair<Peer, List<Message>> -> onActualDataReceived(data) }) { t: Throwable -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        PersistentLogger.logThrowable("LocalJSON issues", Exception(Utils.getCauseIfRuntime(t)))
        showError(view, Utils.getCauseIfRuntime(t))
        resolveRefreshingView(false)
    }

    private fun onActualDataReceived(data: Pair<Peer, List<Message>>) {
        mPost.clear()
        mPost.addAll(data.second)
        mCached.clear()
        mCached.addAll(data.second)
        peer = data.first
        resolveToolbar()
        view?.notifyDataSetChanged()
        resolveRefreshingView(false)
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView(false)
    }

    private fun resolveRefreshingView(isLoading: Boolean) {
        if (isGuiResumed) {
            view!!.showRefreshing(isLoading)
        }
    }

    @OnGuiCreated
    private fun resolveToolbar() {
        if (isGuiReady) {
            view!!.setToolbarTitle(peer.title)
            view!!.setToolbarSubtitle(getString(R.string.messages_in_json, Utils.safeCountOf(mPost)))
            view!!.displayToolbarAvatar(peer)
            view!!.scroll_pos(0)
        }
    }

    override fun onDestroyed() {
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

    init {
        isAttachments = false
        isMy = false
        peer = Peer(0)
        loadActualData()
    }
}