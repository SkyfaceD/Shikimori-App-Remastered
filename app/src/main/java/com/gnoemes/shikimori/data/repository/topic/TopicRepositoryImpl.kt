package com.gnoemes.shikimori.data.repository.topic

import com.gnoemes.shikimori.data.network.TopicApi
import com.gnoemes.shikimori.data.repository.topic.converter.ForumResponseConverter
import com.gnoemes.shikimori.data.repository.topic.converter.TopicResponseConverter
import com.gnoemes.shikimori.entity.forum.domain.Forum
import com.gnoemes.shikimori.entity.forum.domain.ForumType
import com.gnoemes.shikimori.entity.topic.domain.Topic
import io.reactivex.Single
import javax.inject.Inject

class TopicRepositoryImpl @Inject constructor(
        private val api: TopicApi,
        private val converter: TopicResponseConverter,
        private val forumConverter: ForumResponseConverter
) : TopicRepository {

    override fun getList(page: Int, limit: Int, type: ForumType): Single<List<Topic>> =
            api.getList(page, limit, type)
                    .map(converter)
                    .doOnSuccess { if (it.isNotEmpty() && page > 1) it.toMutableList().removeAt(0) }

    override fun getDetails(id: Long): Single<Topic> = api.getDetails(id).map { converter.convertResponse(it) }

    override fun getForums(): Single<List<Forum>> = api.getForums().map(forumConverter)
}