package biz.dealnote.messenger.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import biz.dealnote.messenger.Constants
import biz.dealnote.messenger.Extra
import biz.dealnote.messenger.R
import biz.dealnote.messenger.activity.ActivityFeatures
import biz.dealnote.messenger.activity.ActivityUtils
import biz.dealnote.messenger.adapter.MessagesAdapter
import biz.dealnote.messenger.adapter.MessagesAdapter.OnMessageActionListener
import biz.dealnote.messenger.api.PicassoInstance
import biz.dealnote.messenger.fragment.base.PlaceSupportMvpFragment
import biz.dealnote.messenger.listener.PicassoPauseOnScrollListener
import biz.dealnote.messenger.model.Message
import biz.dealnote.messenger.model.Peer
import biz.dealnote.messenger.mvp.presenter.LocalJsonToChatPresenter
import biz.dealnote.messenger.mvp.view.ILocalJsonToChatView
import biz.dealnote.messenger.util.Objects
import biz.dealnote.messenger.util.RoundTransformation
import biz.dealnote.messenger.util.Utils
import biz.dealnote.messenger.util.ViewUtils
import biz.dealnote.mvp.core.IPresenterFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class LocalJsonToChatFragment : PlaceSupportMvpFragment<LocalJsonToChatPresenter, ILocalJsonToChatView>(), ILocalJsonToChatView, OnMessageActionListener {
    private var mEmpty: TextView? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAdapter: MessagesAdapter? = null
    private var recyclerView: RecyclerView? = null

    private var Title: TextView? = null
    private var SubTitle: TextView? = null
    private var Avatar: ImageView? = null
    private var EmptyAvatar: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_json_chat, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mEmpty = root.findViewById(R.id.empty)
        val mAttachment: FloatingActionButton = root.findViewById(R.id.goto_button)
        mAttachment.setOnClickListener { presenter!!.togleAttachment(false) }
        mAttachment.setOnLongClickListener {
            presenter!!.togleAttachment(true)
            true
        }

        Title = root.findViewById(R.id.dialog_title)
        SubTitle = root.findViewById(R.id.dialog_subtitle)
        Avatar = root.findViewById(R.id.toolbar_avatar)
        EmptyAvatar = root.findViewById(R.id.empty_avatar_text)

        recyclerView = root.findViewById(android.R.id.list)
        recyclerView?.layoutManager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        recyclerView?.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))
        mSwipeRefreshLayout = root.findViewById(R.id.refresh)
        mSwipeRefreshLayout?.isEnabled = false
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout)
        mAdapter = MessagesAdapter(requireActivity(), emptyList(), this, true)
        recyclerView?.adapter = mAdapter
        resolveEmptyText()
        return root
    }

    override fun scroll_pos(pos: Int) {
        if (Objects.nonNull(recyclerView)) {
            recyclerView!!.scrollToPosition(pos)
        }
    }

    private fun resolveEmptyText() {
        if (Objects.nonNull(mEmpty) && Objects.nonNull(mAdapter)) {
            mEmpty!!.visibility = if (mAdapter!!.itemCount == 0) View.VISIBLE else View.GONE
        }
    }

    override fun displayData(posts: List<Message>) {
        if (Objects.nonNull(mAdapter)) {
            mAdapter!!.items = posts
            resolveEmptyText()
        }
    }

    override fun notifyDataSetChanged() {
        if (Objects.nonNull(mAdapter)) {
            mAdapter!!.notifyDataSetChanged()
            resolveEmptyText()
        }
    }

    override fun notifyDataAdded(position: Int, count: Int) {
        if (Objects.nonNull(mAdapter)) {
            mAdapter!!.notifyItemRangeInserted(position, count)
            resolveEmptyText()
        }
    }

    override fun showRefreshing(refreshing: Boolean) {
        if (Objects.nonNull(mSwipeRefreshLayout)) {
            mSwipeRefreshLayout!!.isRefreshing = refreshing
        }
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<LocalJsonToChatPresenter> = object : IPresenterFactory<LocalJsonToChatPresenter> {
        override fun create(): LocalJsonToChatPresenter {
            return LocalJsonToChatPresenter(
                    requireArguments().getInt(Extra.ACCOUNT_ID),
                    requireActivity(),
                    saveInstanceState)
        }
    }

    override fun displayToolbarAvatar(peer: Peer) {
        Avatar?.setOnClickListener {
            presenter?.fireOwnerClick(peer.id)
        }
        if (Utils.nonEmpty(peer.avaUrl)) {
            EmptyAvatar?.visibility = View.GONE
            PicassoInstance.with()
                    .load(peer.avaUrl)
                    .transform(RoundTransformation())
                    .into(Avatar)
        } else {
            PicassoInstance.with().cancelRequest(Avatar!!)
            EmptyAvatar?.visibility = View.VISIBLE
            var name: String = peer.title
            if (name.length > 2) name = name.substring(0, 2)
            name = name.trim { it <= ' ' }
            EmptyAvatar?.text = name
            Avatar?.setImageBitmap(RoundTransformation().transform(Utils.createGradientChatImage(200, 200, peer.id)))
        }
    }

    override fun setToolbarTitle(title: String?) {
        val actionBar = ActivityUtils.supportToolbarFor(this)
        if (Objects.nonNull(actionBar)) {
            actionBar!!.title = null
        }
        Title?.text = title
    }

    override fun setToolbarSubtitle(subtitle: String?) {
        val actionBar = ActivityUtils.supportToolbarFor(this)
        if (Objects.nonNull(actionBar)) {
            actionBar!!.subtitle = null
        }
        SubTitle?.text = subtitle
    }

    override fun onResume() {
        super.onResume()
        ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity())
    }

    override fun onAvatarClick(message: Message, userId: Int) {
        presenter!!.fireOwnerClick(userId)
    }

    override fun onLongAvatarClick(message: Message, userId: Int) {}
    override fun onRestoreClick(message: Message, position: Int) {}
    override fun onMessageLongClick(message: Message): Boolean {
        return false
    }

    override fun onMessageClicked(message: Message) {}
    override fun onMessageDelete(message: Message) {}

    companion object {
        @JvmStatic
        fun newInstance(accountId: Int): LocalJsonToChatFragment {
            val args = Bundle()
            args.putInt(Extra.ACCOUNT_ID, accountId)
            val fragment = LocalJsonToChatFragment()
            fragment.arguments = args
            return fragment
        }
    }
}